package io.pager.validation

import io.pager.PagerError
import io.pager.PagerError.NotFound
import io.pager.client.github.{ GitHubClient, GitHubRelease }
import io.pager.logging.Logger
import io.pager.subscription.Repository.Name
import io.pager.validation.GitHubRepositoryValidatorTestCases._
import io.pager.validation.RepositoryValidator.GitHubRepositoryValidator
import zio._
import zio.test.Assertion._
import zio.test._

object GitHubRepositoryValidatorSpec extends DefaultRunnableSpec(suite(specName)(seq: _*))

object GitHubRepositoryValidatorTestCases {
  val specName: String = "GitHubRepositoryValidatorSpec"

  private def buildValidator(client: GitHubClient.Service): RepositoryValidator.Service =
    new GitHubRepositoryValidator {
      override val logger: Logger.Service             = Logger.Test
      override val gitHubClient: GitHubClient.Service = client
    }.repositoryValidator

  private val notFound = NotFound("ololo")

  private val succeedingValidator = buildValidator {
    new GitHubClient.Service {
      override def repositoryExists(name: Name): IO[PagerError, Name]        = IO.succeed(name)
      override def releases(name: Name): IO[PagerError, List[GitHubRelease]] = ???
    }
  }
  private val failingValidator = buildValidator {
    new GitHubClient.Service {
      override def repositoryExists(name: Name): IO[PagerError, Name]        = IO.fail(notFound)
      override def releases(name: Name): IO[PagerError, List[GitHubRelease]] = ???
    }
  }

  val seq = Seq(
    testM("successfully validate existing repository by name") {
      val repo = "zio/zio"

      succeedingValidator
        .validate(repo)
        .map(result => assert(result, equalTo(Name("zio/zio"))))
    },
    testM("fail to validate non-existing portfolio") {
      val repo = "ololo"

      failingValidator
        .validate(repo)
        .flip
        .map(result => assert(result, equalTo(notFound)))
    }
  )
}
