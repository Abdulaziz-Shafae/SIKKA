# Park catalogue fields

Ride.category uses THRILL, FAMILY, KIDS, WATER, ARCADE. Ride.rideFeatures is an array; it is separate from audience/category and thrill level.

Thrill levels accept LOW, MILD, EXTREME from official guides. MEDIUM and HIGH remain accepted for compatibility with existing records; they are internal legacy labels, not an asserted official scale. Do not silently equate MILD with MEDIUM or EXTREME with HIGH. The same vocabulary is accepted in visitor preferences.

Both Ride and Place use land codes: STEAM_TOWN, CITY_OF_THRILLS, TWILIGHT_GARDENS, DISCOVERY_SPRINGS, GRAND_EXPOSITION, VALLEY_OF_FORTUNE, CITADEL.

For companion conditions, adultCompanionRequired=true with companionRequiredBelowHeightCm=130 means a companion is required below 130 cm. True with no threshold means always required. Null means unverified. Recommended ages are guidance, distinct from mandatory minAgeYears/maxAgeYears.

Place.kind now includes SHOP. Dining details are allowed only for RESTAURANT; shopTypes only for SHOP. Collections are JSON arrays persisted in JPA element-collection tables. Existing HTTP routes are unchanged. Updates remain full replacements: send all fields that must be retained.

Sources:
- https://sixflagsqiddiyacity.com/en/explore/park-map
- https://sixflagsqiddiyacity.com/content/dam/sixflags/en/pdfs/accessibility/rides/Accessibility%20Guide%20-%20Water%20Wheel_r.pdf
- https://sixflagsqiddiyacity.com/content/dam/sixflags/en/pdfs/accessibility/rides/Accessibility%20Guide%20-%20Twilight%20Express_r.pdf
- https://sixflagsqiddiyacity.com/content/dam/sixflags/en/pdfs/accessibility/full-guide/SF_EN_Accessibility%20Guide%20full.pdf

No production database records were changed. Verify every published restriction before importing catalogue records. N/A, unverified data, and zero are not interchangeable. Source documents can disagree: the older Adrena-Line guide gives 105 cm while the current map lists 95 cm; do not pick a value without resolving the discrepancy.

## Distance

Existing mapX/mapY have no defined coordinate unit. Do not treat them as latitude/longitude or meters. For a map calibrated to local meters, straight-line distance is Math.hypot(x2-x1, y2-y1). For pixel coordinates, apply a verified meters-per-pixel scale (separate X/Y scales if necessary) before calculating. This measures a straight line, not a walkable route.

Walking distance requires verified path nodes and edges with lengths in meters, attached to ride/shop entrances. Sum edge lengths along the shortest accessible path using Dijkstra; exclude closed or inaccessible paths. Estimate minutes using distanceMeters / walkingMetersPerMinute, with walking speed explicitly configurable. Do not populate invented coordinates or route lengths. This change does not implement routing or invent a scale.

## Database rollout

Existing height/age/weight fields remain unchanged. New columns are nullable; new collection tables are needed. Use the deployment's normal schema migration process (or development Hibernate schema update). Review existing category values before editing old records because category now has a restricted vocabulary. Back up data before applying a production migration.
