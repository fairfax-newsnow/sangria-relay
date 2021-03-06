package sangria.relay.starWars

import org.scalatest.{Matchers, WordSpec}
import sangria.execution.Executor
import sangria.parser.QueryParser
import sangria.relay.starWars.StarWarsData.ShipRepo
import sangria.relay.util.AwaitSupport

import scala.util.Success

import scala.concurrent.ExecutionContext.Implicits.global

class StarWarsConnectionSpec extends WordSpec with Matchers with AwaitSupport {
  "Connection" when {
    "Fetching" should {
      "Correctly fetches the first ship of the rebels" in {
        val Success(doc) = QueryParser.parse(
          """
            query RebelsShipsQuery {
              rebels {
                name,
                ships(first: 1) {
                  edges {
                    node {
                      name
                    }
                  }
                }
              }
            }
          """)

        Executor.execute(StarWarsSchema.schema, doc, userContext = new ShipRepo).await should be(
          Map(
            "data" → Map(
              "rebels" → Map(
                "name" → "Alliance to Restore the Republic",
                "ships" → Map(
                  "edges" → List(
                    Map(
                      "node" → Map(
                        "name" → "X-Wing"
                      )
                    )
                  )
                )
              ))))
      }

      "Correctly fetches the first two ships of the rebels with a cursor" in {
        val Success(doc) = QueryParser.parse(
          """
            query MoreRebelShipsQuery {
              rebels {
                name,
                ships(first: 2) {
                  edges {
                    cursor,
                    node {
                      name
                    }
                  }
                }
              }
            }
          """)

        Executor.execute(StarWarsSchema.schema, doc, userContext = new ShipRepo).await should be(
          Map(
            "data" → Map(
              "rebels" → Map(
                "name" → "Alliance to Restore the Republic",
                "ships" → Map(
                  "edges" → List(
                    Map(
                      "cursor" → "YXJyYXljb25uZWN0aW9uOjA=",
                      "node" → Map(
                        "name" → "X-Wing"
                      )
                    ),
                    Map(
                      "cursor" → "YXJyYXljb25uZWN0aW9uOjE=",
                      "node" → Map(
                        "name" → "Y-Wing"
                      )
                    )
                  )
                )
              ))))
      }

      "Correctly fetches the next three ships of the rebels with a cursor" in {
        val Success(doc) = QueryParser.parse(
          """
            query EndOfRebelShipsQuery {
              rebels {
                name,
                ships(first: 3 after: "YXJyYXljb25uZWN0aW9uOjE=") {
                  edges {
                    cursor,
                    node {
                      name
                    }
                  }
                }
              }
            }
          """)

        Executor.execute(StarWarsSchema.schema, doc, userContext = new ShipRepo).await should be(
          Map(
            "data" → Map(
              "rebels" → Map(
                "name" → "Alliance to Restore the Republic",
                "ships" → Map(
                  "edges" → List(
                    Map(
                      "cursor" → "YXJyYXljb25uZWN0aW9uOjI=",
                      "node" → Map(
                        "name" → "A-Wing"
                      )
                    ),
                    Map(
                      "cursor" → "YXJyYXljb25uZWN0aW9uOjM=",
                      "node" → Map(
                        "name" → "Millenium Falcon"
                      )
                    ),
                    Map(
                      "cursor" → "YXJyYXljb25uZWN0aW9uOjQ=",
                      "node" → Map(
                        "name" → "Home One"
                      )
                    )
                  )
                )
              ))))
      }

      "Correctly fetches no ships of the rebels at the end of connection" in {
        val Success(doc) = QueryParser.parse(
          """
            query RebelsQuery {
              rebels {
                name,
                ships(first: 3 after: "YXJyYXljb25uZWN0aW9uOjQ=") {
                  edges {
                    cursor,
                    node {
                      name
                    }
                  }
                }
              }
            }
          """)

        Executor.execute(StarWarsSchema.schema, doc, userContext = new ShipRepo).await should be(
          Map(
            "data" → Map(
              "rebels" → Map(
                "name" → "Alliance to Restore the Republic",
                "ships" → Map(
                  "edges" → Nil)))))
      }

      "Correctly identifies the end of the list" in {
        val Success(doc) = QueryParser.parse(
          """
            query EndOfRebelShipsQuery {
              rebels {
                name,
                originalShips: ships(first: 2) {
                  edges {
                    node {
                      name
                    }
                  }
                  pageInfo {
                    hasNextPage
                  }
                }
                moreShips: ships(first: 3 after: "YXJyYXljb25uZWN0aW9uOjE=") {
                  edges {
                    node {
                      name
                    }
                  }
                  pageInfo {
                    hasNextPage
                  }
                }
              }
            }
          """)

        Executor.execute(StarWarsSchema.schema, doc, userContext = new ShipRepo).await should be(
          Map(
            "data" → Map(
              "rebels" → Map(
                "name" → "Alliance to Restore the Republic",
                "originalShips" → Map(
                  "edges" → List(
                    Map(
                      "node" → Map(
                        "name" → "X-Wing"
                      )
                    ),
                    Map(
                      "node" → Map(
                        "name" → "Y-Wing"
                      )
                    )
                  ),
                  "pageInfo" → Map(
                    "hasNextPage" → true
                  )
                ),
                "moreShips" → Map(
                  "edges" → List(
                    Map(
                      "node" → Map(
                        "name" → "A-Wing"
                      )
                    ),
                    Map(
                      "node" → Map(
                        "name" → "Millenium Falcon"
                      )
                    ),
                    Map(
                      "node" → Map(
                        "name" → "Home One"
                      )
                    )
                  ),
                  "pageInfo" → Map(
                    "hasNextPage" → false
                  )
                )
              ))))
      }
    }
  }
}
