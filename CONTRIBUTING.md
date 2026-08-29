# Contributing

Thanks for taking an interest. This is a small project, so the process is short.

## Getting set up

You need **JDK 17** and nothing else — the Maven wrapper fetches Maven, jOOQ
generates its classes from the JPA entity model rather than a live database, and
every test runs against in-memory H2.

```bash
git clone https://github.com/vivekkumarq/ParkingLotManagement.git
cd ParkingLotManagement
./mvnw -B clean verify
```

If that passes, you are ready. To run the application without installing a
database:

```bash
./mvnw -pl parkinglotmanagement-application spring-boot:run \
       -Dspring-boot.run.profiles=dev
```

## Where code goes

The five modules have a strict one-way dependency chain. A change usually touches
several of them in this order:

| You are adding | Put it in |
|---|---|
| A DTO, an enum, an exception, a service interface | `parkinglotmanagement-api` |
| A schema change | `parkinglotmanagement-database` (a new `V{n}__*.sql`) |
| A query, a service implementation, business logic | `parkinglotmanagement-impl` |
| An endpoint | `parkinglotmanagement-web` |
| Configuration, a scheduled job, a profile | `parkinglotmanagement-application` |

Nothing may depend on `-web` or `-application`. If you find yourself wanting to,
the thing you need probably belongs in `-api`.

## Changing the schema

1. **Never edit a migration that already exists.** It may have been applied to
   someone's database. Add a new `V{n}__Description.sql`.
2. Make the matching change to the JPA entity in `-api` — that model is the input
   to jOOQ code generation, so the generated classes will not know about a column
   the entity does not declare.
3. Run `./mvnw clean verify`. `SchemaMigrationTest` applies the real migrations to
   H2 and drives the repositories against the result, so drift between the
   migration and the entity fails the build.

Migrations run on **both PostgreSQL and H2**, so keep to DDL both accept:
`ADD COLUMN`, `DROP COLUMN`, `SET NOT NULL`, `SET DEFAULT`, `ADD CONSTRAINT`,
`CREATE INDEX`, and plain `UPDATE` for moving data. `ALTER COLUMN ... TYPE ...
USING` is PostgreSQL-only — V2 retypes columns through a temporary column and a
`CAST` for exactly this reason.

## Style

- Money is **always** `BigDecimal`, scaled to 2 decimals with `HALF_UP`. Never
  `double`, never `float`.
- Services are `@Transactional`; read paths are `@Transactional(readOnly = true)`.
- Services throw subclasses of `ParkingLotException`. They never know about HTTP
  status codes — `GlobalExceptionHandler` maps them.
- Repositories return `Optional` for a single row and never `null`.
- Read "now" from the injected `Clock`, not `LocalDateTime.now()`. That is what
  makes the time-dependent logic testable.
- User input reaching a query goes through the RSQL whitelist. Never concatenate
  a request value into SQL.
- Comments explain *why*, not *what*. If a line needs a comment to say what it
  does, rename something instead.

## Tests

New behaviour needs tests. Match the existing split:

- **Unit tests** (`*Test`, Mockito) for logic that does not need a database —
  `FeeCalculatorTest`, `SlotAllocationServiceImplTest`, `CrudServiceTest`.
- **Integration tests** (`@IntegrationTest`, H2 + Flyway) for anything involving
  SQL. Use `DatabaseFixture` to build a car park and clean up in `@AfterEach`.
- **Controller tests** (`@WebMvcTest`) for endpoints, with the service mocked.
  Assert the status code, the body *and* the error shape.

`./mvnw -B clean verify` must pass. Do not skip tests to make it green.

Bug fixes should come with a test that fails without the fix. Several of the
existing tests exist precisely because they caught a real bug — see
`ReservationServiceIntegrationTest.expiryRespectsOtherHolders`.

## Commits and pull requests

- Conventional commits: `feat(scope):`, `fix(scope):`, `test(scope):`,
  `refactor(scope):`, `docs:`, `build:`, `chore:`.
- One logical change per commit. The body should say *why*, not restate the diff.
- Open the pull request against `main`. CI runs the full reactor build on JDK 17;
  it must be green.

## Documenting

The README documents only what is implemented and works. If you add a feature,
add it there — endpoint table, configuration table, and a diagram if it changes
the architecture or a lifecycle. If you remove one, take the claim out. Please do
not describe anything as working that you have not run.
