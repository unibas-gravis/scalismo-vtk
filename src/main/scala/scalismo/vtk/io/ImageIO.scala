/*
 * Copyright 2015 University of Basel, Graphics and Vision Research Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scalismo.vtk.io

import breeze.linalg
import breeze.linalg.{DenseMatrix, DenseVector, diag}
import scalismo.common.Scalar
import scalismo.geometry.*
import scalismo.image.{DiscreteImage, DiscreteImageDomain, StructuredPoints, StructuredPoints3D}
import scalismo.io.ImageIO.readNifti
import scalismo.utils.ImageConversion.{VtkAutomaticInterpolatorSelection, VtkInterpolationMode}
import scalismo.utils.{CanConvertToVtk, ImageConversion}
import spire.math.{UByte, UInt, UShort}
import vtk.*

import java.io.{File, IOException}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

/**
 * Implements methods for reading and writing D-dimensional images
 *
 * '''WARNING! WE ARE USING an LPS WORLD COORDINATE SYSTEM'''
 *
 * VTK file format does not indicate the orientation of the image. Therefore, when reading from VTK, we assume that it
 * is in RAI orientation. Hence, no magic is done, the same information (coordinates) present in the VTK file header are
 * directly mapped to our coordinate system.
 *
 * This is also the case when writing VTK. Our image domain information (origin, spacing ..) is mapped directly into the
 * written VTK file header.
 *
 * This is however not the case for Nifti files! Nifti file headers contain an affine transform from the ijk image
 * coordinates to an RAS World Coordinate System (therefore supporting different image orientations). In order to read
 * Nifti files coherently, we need to adapt the obtained RAS coordinates to our LPS system :
 *
 * This is done by mirroring the first two dimensions of each point after applying the affine transform
 *
 * The same mirroring is done again when writing an image to the Nifti format.
 *
 * '''Note on Nifti's qform and sform :'''
 *
 * As mentioned above, the Nifti header contains a transform from the unit ijk grid to the RAS world coordinates of the
 * grid. This transform can be encoded in 2 entries of the Nifti header, the qform and the sform. In some files, these 2
 * entries can both be present, and in some cases could even indicate different transforms. In Scalismo, when such a
 * case happens, we favour the sform entry by default. If you wish instead to favour the qform transform, you can do so
 * by setting a flag appropriately in the [[scalismo.io.ImageIO.read3DScalarImage]] method.
 *
 * ''' Documentation on orientation :'''
 *
 * http://www.grahamwideman.com/gw/brain/orientation/orientterms.htm
 *
 * http://www.slicer.org/slicerWiki/index.php/Coordinate_systems
 *
 * http://brainder.org/2012/09/23/the-nifti-file-format/
 */
object ImageIO {

  /**
   * Read a 3D Scalar Image
   * @param file
   *   image file to be read
   * @tparam S
   *   Voxel type of the image
   */
  def read3DScalarImage[S: Scalar: ClassTag](
    file: File
  ): Try[DiscreteImage[_3D, S]] = {

    file match {
      case f if f.getAbsolutePath.endsWith(".vtk") =>
        val reader = new vtkStructuredPointsReader()
        reader.SetFileName(f.getAbsolutePath)
        reader.Update()
        val errCode = reader.GetErrorCode()
        if (errCode != 0) {
          return Failure(
            new IOException(
              s"Failed to read vtk file ${f.getAbsolutePath}. " +
                s"(error code from vtkReader = $errCode)"
            )
          )
        }
        val sp = reader.GetOutput()
        val img = ImageConversion.vtkStructuredPointsToScalarImage[_3D, S](sp)
        reader.Delete()
        sp.Delete()
        // unfortunately, there may still be VTK leftovers, so run garbage collection
        vtkObjectBase.JAVA_OBJECT_MANAGER.gc(false)
        img
      case f if f.getAbsolutePath.endsWith(".nii") || f.getAbsolutePath.endsWith(".nia") =>
        readNifti[S](f, favourQform = false)
      case _ => Failure(new Exception("Unknown file type received" + file.getAbsolutePath))
    }
  }

  /**
   * Read a 3D Scalar Image, and possibly convert it to the requested voxel type.
   *
   * This method is similar to the [[read3DScalarImage]] method, except that it will convert the image to the requested
   * voxel type if the type in the file is different, whereas [[read3DScalarImage]] will throw an exception in that
   * case.
   *
   * @param file
   *   image file to be read
   * @tparam S
   *   Voxel type of the image
   */
  def read3DScalarImageAsType[S: Scalar: ClassTag](
    file: File
  ): Try[DiscreteImage[_3D, S]] = {
    def loadAs[T: Scalar: ClassTag]: Try[DiscreteImage[_3D, T]] = {
      read3DScalarImage[T](file)
    }

    val result = (for {
      fileScalarType <- ScalarDataType.ofFile(file)
    } yield {
      val expectedScalarType = ScalarDataType.fromType[S]
      if (expectedScalarType == fileScalarType) {
        loadAs[S]
      } else {
        val s = implicitly[Scalar[S]]
        fileScalarType match {
          case ScalarDataType.Byte   => loadAs[Byte].map(_.map(s.fromByte))
          case ScalarDataType.Short  => loadAs[Short].map(_.map(s.fromShort))
          case ScalarDataType.Int    => loadAs[Int].map(_.map(s.fromInt))
          case ScalarDataType.Float  => loadAs[Float].map(_.map(s.fromFloat))
          case ScalarDataType.Double => loadAs[Double].map(_.map(s.fromDouble))
          case ScalarDataType.UByte  => loadAs[UByte].map(_.map(u => s.fromShort(u.toShort)))
          case ScalarDataType.UShort => loadAs[UShort].map(_.map(u => s.fromInt(u.toInt)))
          case ScalarDataType.UInt   => loadAs[UInt].map(_.map(u => s.fromLong(u.toLong)))

          case _ => Failure(new IllegalArgumentException(s"unknown scalar type $fileScalarType"))
        }
      }
    }).flatten
    result
  }

  /**
   * Read a 2D Scalar Image
   * @param file
   *   image file to be read
   * @tparam S
   *   Voxel type of the image
   */
  def read2DScalarImage[S: Scalar: ClassTag](file: File): Try[DiscreteImage[_2D, S]] = {

    file match {
      case f if f.getAbsolutePath.endsWith(".vtk") =>
        val reader = new vtkStructuredPointsReader()
        reader.SetFileName(f.getAbsolutePath)
        reader.Update()
        val errCode = reader.GetErrorCode()
        if (errCode != 0) {
          return Failure(
            new IOException(
              s"Failed to read vtk file ${file.getAbsolutePath}. " +
                s"(error code from vtkReader = $errCode"
            )
          )
        }
        val sp = reader.GetOutput()
        val img = ImageConversion.vtkStructuredPointsToScalarImage[_2D, S](sp)
        reader.Delete()
        sp.Delete()
        // unfortunately, there may still be VTK leftovers, so run garbage collection
        vtkObjectBase.JAVA_OBJECT_MANAGER.gc(false)
        img

      case _ => Failure(new Exception("Unknown file type received" + file.getAbsolutePath))
    }
  }


  def writeVTK[D: NDSpace: CanConvertToVtk, S: Scalar: ClassTag](img: DiscreteImage[D, S],
                                                                 file: File,
                                                                 interpolationMode: VtkInterpolationMode =
                                                                   VtkAutomaticInterpolatorSelection
  ): Try[Unit] = {

    val imgVtk = ImageConversion.imageToVtkStructuredPoints(img, interpolationMode)

    val writer = new vtkStructuredPointsWriter()
    writer.SetInputData(imgVtk)
    writer.SetFileName(file.getAbsolutePath)
    writer.SetFileTypeToBinary()
    writer.Update()
    val errorCode = writer.GetErrorCode()

    // unfortunately, there will probably still be VTK leftovers from objects allocated
    // outside of our control, so run garbage collection
    vtkObjectBase.JAVA_OBJECT_MANAGER.gc(false)

    if (errorCode != 0) {
      Failure(new IOException(s"Error writing vtk file ${file.getAbsolutePath} (error code $errorCode"))
    } else {
      Success(())
    }
  }

}
