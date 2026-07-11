# ADR 0001: Risk-Based Tests

## Status

Accepted on 2026-07-09.

## Decision

SparkFactionAPI keeps focused Java tests for pure contracts such as policy
ordering, fallback behavior, allocation, version payloads, and public API
compatibility. Runtime-only mixin and Minecraft lifecycle behavior remains an
integration or manual verification responsibility.

Tests must not require production reset hooks, widen production visibility only
for test access, or encode incidental implementation structure.

## Rationale

A blanket ban on committed tests left compatibility-sensitive public semantics
without durable regression checks. A blanket requirement to unit-test every
mixin would create brittle tests that do not prove real injection behavior. The
risk-based split protects stable contracts while keeping runtime verification
honest.
