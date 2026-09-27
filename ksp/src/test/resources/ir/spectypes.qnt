// Fixture for SpecTypesGeneratorTest (qk-ixox). Regenerate its IR with:
//   quint typecheck --out ksp/src/test/resources/ir/spectypes.ir.json ksp/src/test/resources/ir/spectypes.qnt
module lib {
  type Coin = { denom: str, amount: int }
}

module spectypes {
  import lib.*

  type Option[a] = Some(a) | None
  type Opt[a] = Present(a) | Absent
  type Shape = Circle({ r: int }) | Square(int) | Dot
  type Id = int
  type Mixed = { pair: (int, str) }
  type Purse = { held: Opt[Coin] }

  var wallet: Coin
  var maybe: Option[Id]
  var shapes: Set[Shape]
  var grid: (int, int) -> Shape
  var owner: { name: str, tags: Set[str] }
  var flags: List[bool]
  var mine: Opt[Coin]
  var purse: Purse

  action init = all {
    wallet' = { denom: "a", amount: 1 },
    maybe' = None,
    shapes' = Set(),
    grid' = Map(),
    owner' = { name: "n", tags: Set() },
    flags' = [],
    mine' = Absent,
    purse' = { held: Absent },
  }

  action paint = {
    nondet s = Set(Dot, Square(1), Circle({ r: 2 })).oneOf()
    nondet at = Set({ x: 1, y: 2 }).oneOf()
    all {
      wallet' = wallet,
      maybe' = maybe,
      shapes' = shapes.union(Set(s)),
      grid' = grid.put((at.x, at.y), s),
      owner' = owner,
      flags' = flags,
      mine' = mine,
      purse' = purse,
    }
  }

  action step = any { paint }
}
