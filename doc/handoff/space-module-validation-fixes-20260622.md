# Space Module Validation Fixes — Context Record

## Saved
2026-06-22

## Task
Audited all 11 space management modules against csyh reference project, identified validation/field gaps, fixed 8 modules, committed.

## Changes Applied (commit `7a1ce84`)
| File | What Changed |
|------|-------------|
| `api/building.ts` | Added `areaId` to `Building` interface + `getBuildingPage` params |
| `views/area/Index.vue` | `areaCovered` + `functionArea` props + required rules |
| `views/building/Index.vue` | Added areaId search filter + form field + `loadAreaOptions` |
| `views/floor/Index.vue` | `serialCode`/`floorCategory`/`coefficient` → props + required rules |
| `views/massif/Index.vue` | `massifName` → prop + required rule |
| `views/room/Index.vue` | `buildingId` → required rule (prop already existed) |
| `views/space-category/Index.vue` | `typeDescribe` → prop + required rule |
| `views/space/Index.vue` | `spaceDescribe`/`areaId`/`categoryId` → props + required rules |

## Skipped
- **kit/Index.vue**: `equipmentList` sub-table needs backend support (no API exists)
- **Async name uniqueness checks**: Need `ApiCheckName`/`ApiCheckSpaceName` backend endpoints — don't exist in current API files

## Modules Verified as Complete (no changes needed)
- `room-purpose/Index.vue`
- `land-nature/Index.vue`
- `plan-use/Index.vue`

## Reference
csyh reference at `D:\work\AI\output\code\csyh\pai-park-space-ui-csyh-2.x\std\pages\*`