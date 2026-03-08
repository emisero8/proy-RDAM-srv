# RDAM System Redesign — Certificado de Deudor Alimentario Moroso

## Planning
- [x] Review current POC (`poc-rdam-v2.html`) and specs
- [x] Create implementation plan
- [x] Get user approval on plan

## Execution
- [x] Update POC HTML (`poc-rdam-v2.html` → `poc-rdam-v3.html`)
  - [x] Rename RDAM branding → "Registro de Deudores Alimentarios Morosos"
  - [x] Redesign Ciudadano views (single certificate type, simplified flow)
  - [x] Redesign Interno views (remove review, add PDF upload)
  - [x] Remove Aprobar/Rechazar modals, add Cargar Certificado modal
  - [x] Update state flow (remove review states, add ASIGNADA/CERT. CARGADO)
  - [x] Update JavaScript logic (nav, roles, state machine, checkout)
- [ ] Update SPEC.md to reflect new domain
- [ ] Update supporting documentation

## Verification
- [x] Verify HTML structure completeness (all 10 views, 2 modals, JS logic)
- [ ] Open POC in browser and verify all role flows
- [ ] Verify Ciudadano can request certificate and see status
- [x] **Refinar Ciudadano Menu y UX** <!-- id: 4 -->
    - [x] Unificar Dashboard y Mis Solicitudes
    - [x] Simplificar navegación (solo 2 opciones)
    - [x] Agregar estado "Expirada" (badge, filtro, ejemplo)
    - [x] Armonizar estética Interno vs Ciudadano
    - [x] Corregir padding y márgenes (consistencia visual)
- [ ] **Despliegue y Pruebas finales** <!-- id: 5 -->
