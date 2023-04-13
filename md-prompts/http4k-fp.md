Principles of functional programming:
- All domain functions must be pure. this mean that their result must only depend on their inputs. In other words, any reading or writing of global variables, singleton stateful objects, or external resources like files or network sockets is not allowed.
  Impure functions must be used inside Reader functors.

- All data must be immutable. This mean only primitives and data class with only `val` fields.
  No open classes. Abstract classes and Interfaces are ok. Sealed classes are ok to model co-product types.

- Functions cannot throw exceptions. If they can fail, they can use an Either as a result type or use null as error.

As design euristics:
Keep functions small and compose them with higher-order-functions
Prefer passing directly the function needed rather than whole interfaces as arguments.
