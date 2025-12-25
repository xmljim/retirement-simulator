/**
 * Distribution strategy implementations.
 *
 * <p>Contains the Strategy pattern implementations for different
 * retirement withdrawal approaches.
 *
 * <h2>Strategies</h2>
 * <ul>
 *   <li>{@code StaticWithdrawalStrategy} - Fixed percentage withdrawal (4% rule)</li>
 *   <li>{@code BucketStrategy} - Time-segmented bucket approach</li>
 *   <li>{@code SpendingCurveStrategy} - Age-based spending adjustments</li>
 *   <li>{@code GuardrailsStrategy} - Dynamic withdrawal with guardrails</li>
 * </ul>
 */
package io.github.xmljim.retirement.simulation.strategy;
