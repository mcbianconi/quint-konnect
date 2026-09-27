// Fixture for SpecActionValidatorTest's dispatchableActionNames coverage: a `step` any{} branch
// bound through a `let` (`nondet n = ...; addTwo(n)`, quint's own desugaring of a nondet binding
// before a call) and a nested `any {...}` (`nested`), on top of fixture.qnt's plain-name case.
module dispatch {
  var value: int

  action addOne = all {
    value' = value + 1,
  }

  action addTwo(n) = all {
    value' = value + n,
  }

  action nested = any {
    addOne,
  }

  action step = any {
    addOne,
    nondet n = Set(1, 2, 3).oneOf()
    addTwo(n),
    nested,
  }

  action init = all {
    value' = 0,
  }
}
