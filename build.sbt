import sbt.Resolver

ThisBuild / version := "1.0.0"

Test / parallelExecution := false

lazy val root = (project in file("."))
  .settings(
    name := "scalismo.vtk",
    organization := "ch.unibas.cs.gravis",
    scalaVersion := "3.3.0",
    homepage := Some(url("https://scalismo.org")),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    scmInfo := Some(
      ScmInfo(url("https://github.com/unibas-gravis/scalismo-vtk"), "git@github.com:unibas-gravis/scalismo-vtk.git")
    ),
    developers := List(
      Developer("marcelluethi", "marcelluethi", "marcel.luethi@unibas.ch", url("https://github.com/marcelluethi")),
      Developer("madsendennis", "madsendennis", "dennis@dentexion.com", url("https://github.com/madsendennis"))
    ),
    publishMavenStyle := true,
    publishTo := Some(
      if (isSnapshot.value)
        Opts.resolver.sonatypeSnapshots
      else
        Opts.resolver.sonatypeStaging
    ),
    resolvers ++= Seq(
      Resolver.jcenterRepo,
      Resolver.sonatypeRepo("snapshots"),
      Resolver.sonatypeRepo("releases")
    ),
    scalacOptions ++= {
      Seq(
        "-encoding",
        "UTF-8",
        "-feature",
        "-language:implicitConversions",
        "-unchecked"
        // disabled during the migration
        // "-Xfatal-warnings"
      )
    },
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.16" % "test",
      "ch.unibas.cs.gravis" %% "scalismo" % "1.0.0",
      "ch.unibas.cs.gravis" % "vtkjavanativesall" % "0.2-RC1"
    )
  )
  .enablePlugins(GitBranchPrompt)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion),
    buildInfoPackage := "scalismo.vtk"
  )
  .enablePlugins(GhpagesPlugin)
  .settings(
    git.remoteRepo := "git@github.com:unibas-gravis/scalismo-vtk.git"
  )
  .enablePlugins(SiteScaladocPlugin)
