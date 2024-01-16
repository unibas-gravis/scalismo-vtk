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
package scalismo.vtk.mesh

import scalismo.common.PointId
import scalismo.geometry.{EuclideanVector, Point, _3D}
import scalismo.mesh.MeshMetrics
import scalismo.utils.Random
import scalismo.vtk.ScalismoTestSuite
import scalismo.vtk.io.MeshIO

import java.io.File
import java.net.URLDecoder

class tetrahedralMeshMetricsTests extends ScalismoTestSuite {

  implicit val rng: Random = Random(42L)
  final val epsilon = 1e-8

  val path = getClass.getResource("/tetraMesh.vtk").getPath
  val mesh = MeshIO.readTetrahedralMesh(new File(URLDecoder.decode(path, "UTF-8"))).get
  val translationLength = 1.0
  val translatedMesh = mesh.transform((pt: Point[_3D]) => pt + EuclideanVector(translationLength, 0.0, 0.0))

  describe("The ProcrustesDistanceMetric") {

    it("yields 0 between the same tetrahedral mesh") {
      MeshMetrics.procrustesDistance(mesh, mesh) should be(0.0 +- epsilon)
    }

    it("should be 0 for a translated mesh") {
      MeshMetrics.procrustesDistance(mesh, translatedMesh) should be(0.0 +- epsilon)
    }

  }

}
