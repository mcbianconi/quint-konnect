// Fixture for QuintIrParserTest: a record var, a sum typedef with a unit variant and a
// record-payload variant, and a `step` action that reaches its `nondet` only through a helper
// action it calls by name (`pick`), not in its own body -- exercises the transitive nondet walk
// (QuintIr.kt's collectNondets) the same way tictactoe.qnt's `MoveX` reaches `Win`/`Block`/etc.
module fixture {
  type Choice = Init | Add({ n: int })

  var value: int
  var config: { limit: int }
  var lastChoice: Choice

  action pick = {
    nondet n = Set(1, 2, 3).oneOf()
    all {
      value' = value + n,
      lastChoice' = Add({ n: n }),
    }
  }

  action init = all {
    value' = 0,
    config' = { limit: 10 },
    lastChoice' = Init,
  }

  action step = any {
    pick,
  }
}
