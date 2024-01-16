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
import scalismo.geometry.Point
import scalismo.mesh.TetrahedronId
import scalismo.vtk.ScalismoTestSuite
import scalismo.vtk.io.MeshIO

import java.io.File
import java.net.URLDecoder
import scala.language.implicitConversions

class MeshBoundaryTests extends ScalismoTestSuite {

  describe("a tetrahedral mesh boundary") {
    it("should return the correct labels for the tetrahedrons in the test mesh") {

      val path = getClass.getResource("/tetraMesh.vtk").getPath
      val tmesh = MeshIO.readTetrahedralMesh(new File(URLDecoder.decode(path, "UTF-8"))).get

      val tetsOnBoundary = IndexedSeq(0, 1, 3, 4, 8, 9, 11, 12, 16, 17, 19, 20, 24, 25, 27, 28, 32, 33, 35, 36, 40, 41,
                                      43, 47, 48, 49, 51, 52, 56, 57, 59, 60, 64, 65, 67, 68, 72, 73, 75, 76, 80, 81,
                                      83, 84, 88, 89, 91, 95, 96, 97, 99, 100, 104, 105, 107, 111, 112, 113, 115, 116,
                                      120, 121, 123, 124, 128, 129, 131, 132, 136, 137, 139, 140, 144, 145, 147, 148,
                                      152, 153, 155, 156).map(i => TetrahedronId(i))
      tetsOnBoundary.map { tet =>
        require(tmesh.operations.tetrahedronIsOnBoundary(tet))
      }

      val tetsInside = IndexedSeq(2, 5, 6, 7, 10, 13, 14, 15, 18, 21, 22, 23, 26, 29, 30, 31, 34, 37, 38, 39, 42, 44,
                                  45, 46, 50, 53, 54, 55, 58, 61, 62, 63, 66, 69, 70, 71, 74, 77, 78, 79, 82, 85, 86,
                                  87, 90, 92, 93, 94, 98, 101, 102, 103, 106, 108, 109, 110, 114, 117, 118, 119, 122,
                                  125, 126, 127, 130, 133, 134, 135, 138, 141, 142, 143, 146, 149, 150, 151, 154, 157,
                                  158, 159).map(i => TetrahedronId(i))
      tetsInside.map { tet =>
        require(!tmesh.operations.tetrahedronIsOnBoundary(tet))
      }
    }

    it("should return the correct labels for the points in the test mesh") {

      val path = getClass.getResource("/tetraMesh.vtk").getPath
      val tmesh = MeshIO.readTetrahedralMesh(new File(URLDecoder.decode(path, "UTF-8"))).get

      val innerPoints = IndexedSeq(12, 14, 15, 18, 20, 21, 24, 26, 33, 35, 36, 45, 51).map(i => PointId(i))
      innerPoints.map { pid =>
        require(!tmesh.operations.pointIsOnBoundary(pid))
      }

      val surfacePoints =
        IndexedSeq(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 16, 17, 19, 22, 23, 25, 27, 28, 29, 30, 31, 32, 34, 37, 38,
                   39, 40, 41, 42, 43, 44, 46, 47, 48, 49, 50, 52, 53, 54).map(i => PointId(i))
      surfacePoints.map { pid =>
        require(tmesh.operations.pointIsOnBoundary(pid))
      }
    }
  }
}
