Principles of functional programming:
- All domain functions must be pure. this mean that their result must only depend on their inputs. In other words, any reading or writing of global variables, singleton stateful objects, or external resources like files or network sockets is not allowed.
  Impure functions must be used inside Reader functors.

- All data must be immutable. This mean that the only data allowed are primitives and data class with only `val` fields. Mutable maps and collections are acceptable if they stay inside a method, but any class field or external argument should use read-only collections and maps. Folds and reduce are preferable over for loops. Avoid mutable variable inside functions.
 Don't use open classes. Abstract classes and Interfaces are ok. Sealed classes are great to model co-product types.

- Functions cannot throw exceptions. If they can fail, they can use an Either as a result type or use null as error.

As design euristics:
Keep functions small and compose them with higher-order-functions.
Prefer solutions using tail recursion to simplify problems.
Prefer using expression syntax rather functions with statements and temporary variables.
Prefer passing directly the function needed rather than whole interfaces as arguments.
