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
package scalismo.vtk.image

import org.scalatest.PrivateMethodTester
import scalismo.common.PointId
import scalismo.common.interpolation.{BSplineImageInterpolator1D, BSplineImageInterpolator2D, BSplineImageInterpolator3D}
import scalismo.geometry.*
import scalismo.geometry.EuclideanVector.implicits.*
import scalismo.geometry.IntVector.implicits.*
import scalismo.geometry.Point.implicits.*
import scalismo.vtk.ScalismoTestSuite
import scalismo.vtk.io.ImageIO

import java.io.File
import java.net.URLDecoder
import scala.language.implicitConversions

class InterpolationTest extends ScalismoTestSuite with PrivateMethodTester {

  describe("A 2D interpolation  Spline") {
    it("Interpolates the values correctly for a test dataset") {
      val testImgUrl = getClass.getResource("/lena256.vtk").getPath
      val discreteFixedImage = ImageIO.read2DScalarImage[Short](new File(URLDecoder.decode(testImgUrl, "UTF-8"))).get
      val interpolatedImage = discreteFixedImage.interpolate(BSplineImageInterpolator2D[Short](2))

      for ((p, i) <- discreteFixedImage.domain.pointSet.points.zipWithIndex) {
        interpolatedImage(p).toShort should be(discreteFixedImage(PointId(i)) +- 30)
      }
    }
  }
}
