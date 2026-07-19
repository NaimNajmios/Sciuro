# Test Notes: Phase A4 - LLM Fallback

## Scope
- Integrated Ktor client and Kotlinx Serialization.
- Built `LlmFallbackParser` connecting to Groq OpenAI-compatible API.
- Built `SciuroParserPipeline` to chain regex and LLM.

## Results
- `core-parsing` builds successfully.
- Proper fallback logic defined, bypassing LLM if `apiKey` is null.

## Excluded
- Integration tests simulating real network calls to Groq are omitted to prevent CI failures when API keys are not present.
