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

import niftijio.NiftiHeader
import scalismo.common.Scalar
import scalismo.io.FastReadOnlyNiftiVolume
import scalismo.vtk.utils.VtkHelpers
import spire.math.{UByte, UInt, UShort}
import vtk.{vtkObjectBase, vtkStructuredPointsReader}
import scalismo.io.ScalarDataType

import java.io.{File, IOException}
import scala.reflect.ClassTag
import scala.util.{Failure, Try}

/**
 * An enumeration comprising all the data types that we can read and write, in VTK and Nifti formats.
 */
private[scalismo] object ScalarDataTypeVTK {

  /**
   * Return the ScalarType value corresponding to the data present in a given file. Only .vtk, .nii and .nia files are
   * supported.
   * @param file
   *   the file to check
   * @return
   *   the scalar type present in the given file, wrapped in a [[scala.util.Success]], or a [[scala.util.Failure]]
   *   explaining the error.
   */
  def ofFile(file: File): Try[ScalarDataType.Value] = {
    val fn = file.getName
    if (fn.endsWith(".nii") || fn.endsWith(".nia")) {
      FastReadOnlyNiftiVolume.getScalarType(file)
    } else if (fn.endsWith(".vtk")) Try {
      val reader = new vtkStructuredPointsReader
      reader.SetFileName(file.getAbsolutePath)
      reader.Update()
      val errCode = reader.GetErrorCode()
      if (errCode != 0) {
        reader.Delete()
        throw new IOException(
          s"Failed to read vtk file ${file.getAbsolutePath}. (error code from vtkReader = $errCode)"
        )
      }
      val st = reader.GetOutput().GetScalarType()
      reader.Delete()
      // prevent memory leaks
      vtkObjectBase.JAVA_OBJECT_MANAGER.gc(false)
      ScalarDataType.fromVtkId(st)
    }
    else {
      Failure(new Exception(s"File $file: unsupported file extension"))
    }
  }

}
