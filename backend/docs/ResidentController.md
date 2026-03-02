# ResidentController

`ResidentController` is a REST controller that exposes the resident registry API at `/api/v1/residents`.

## Access Control

- All endpoints require `CLERK` or `ADMIN` role (`@PreAuthorize` at class level)
- JWT bearer token required on all endpoints

## Endpoints

| Method | Path                                        | Description                                                              |
| ------ | ------------------------------------------- | ------------------------------------------------------------------------ |
| `GET`  | `/api/v1/residents`                         | Paginated search by name (`q`) and/or `purok`; sorted by last/first name |
| `POST` | `/api/v1/residents`                         | Creates a walk-in resident (no portal account)                           |
| `GET`  | `/api/v1/residents/{id}`                    | Fetch a single resident by UUID                                          |
| `PUT`  | `/api/v1/residents/{id}`                    | Update resident fields                                                   |
| `GET`  | `/api/v1/residents/pending-users`           | Lists residents whose portal accounts are awaiting verification          |
| `POST` | `/api/v1/residents/users/{userId}/activate` | Approves a pending portal account → sets status to `ACTIVE`              |
| `POST` | `/api/v1/residents/users/{userId}/reject`   | Rejects a pending portal account → sets status to `REJECTED`             |

## Design Notes

- Delegates all business logic to `ResidentService` — the controller only handles HTTP concerns
- Search uses Spring's `PageRequest` with `Sort` and returns `PageResponse<ResidentDTO>` (the project's custom wrapper, not Spring's raw `Page<T>`)
- The `pending-users` / `activate` / `reject` endpoints support the staff workflow for approving self-registered residents from the portal — staff can review and approve or deny access
