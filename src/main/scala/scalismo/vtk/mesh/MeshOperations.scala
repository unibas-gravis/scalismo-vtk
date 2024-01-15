/*
 * Copyright 2016 University of Basel, Graphics and Vision Research Group
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
package scalismo.vtk.mesh

import scalismo.geometry._3D
import scalismo.vtk.utils.MeshConversion
import scalismo.mesh.TriangleMesh

import scala.collection.parallel.immutable.ParVector

object MeshOperations {
  def apply(mesh: TriangleMesh[_3D]) = new TriangleMesh3DOperations(mesh)
}

class TriangleMesh3DOperations(private val mesh: TriangleMesh[_3D]) {

  /**
   * Attempts to reduce the number of vertices of a mesh to the given number of vertices.
   *
   * @param targetedNumberOfVertices
   *   The targeted number of vertices. Note that it is not guaranteed that this number is reached exactly
   * @return
   *   The decimated mesh
   */
  def decimate(targetedNumberOfVertices: Int): TriangleMesh[_3D] = {
    val refVtk = MeshConversion.meshToVtkPolyData(mesh)
    val decimate = new vtk.vtkQuadricDecimation()

    val reductionRate = 1.0 - (targetedNumberOfVertices / mesh.pointSet.numberOfPoints.toDouble)

    decimate.SetTargetReduction(reductionRate)

    decimate.SetInputData(refVtk)
    decimate.Update()
    val decimatedRefVTK = decimate.GetOutput()
    MeshConversion.vtkPolyDataToTriangleMesh(decimatedRefVTK).get
  }
}
