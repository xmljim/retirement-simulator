/**
 * Integration tests for the retirement simulator domain.
 *
 * <h2>Integration vs Unit Tests</h2>
 *
 * <p><b>Unit tests</b> (in other packages) test individual classes in isolation,
 * often using mocks or stubs for dependencies. They verify that a single component
 * behaves correctly given specific inputs.
 *
 * <p><b>Integration tests</b> (in this package) test multiple components working
 * together as a system. They verify that the components integrate correctly and
 * produce expected outcomes for realistic scenarios.
 *
 * <h2>Tests in This Package</h2>
 *
 * <table border="1">
 *   <tr><th>Test Class</th><th>Purpose</th><th>Components Tested</th></tr>
 *   <tr>
 *     <td>{@link M5IntegrationTest}</td>
 *     <td>M5 Expense &amp; Budget Modeling flows</td>
 *     <td>Expense categories, inflation, lifecycle phases</td>
 *   </tr>
 *   <tr>
 *     <td>{@link M6bStrategyIntegrationTest}</td>
 *     <td>M6b spending strategies with orchestrator</td>
 *     <td>SpendingStrategy + SpendingOrchestrator + Sequencer</td>
 *   </tr>
 *   <tr>
 *     <td>{@link M6EndToEndIntegrationTest}</td>
 *     <td>Complete M6 withdrawal flows</td>
 *     <td>All M6 strategies, RMD integration, multi-year scenarios</td>
 *   </tr>
 *   <tr>
 *     <td>{@link ContributionIntegrationTest}</td>
 *     <td>M3a contribution limit checking</td>
 *     <td>ContributionRouter + LimitChecker + YTDTracker</td>
 *   </tr>
 *   <tr>
 *     <td>{@link PhaseOutIntegrationTest}</td>
 *     <td>M3b MAGI to phase-out flow</td>
 *     <td>MAGICalculator + PhaseOutCalculator</td>
 *   </tr>
 * </table>
 *
 * <h2>When to Add Integration Tests</h2>
 *
 * <p>Add integration tests when:
 * <ul>
 *   <li>Multiple components must work together to fulfill a use case</li>
 *   <li>Testing realistic end-to-end scenarios (e.g., full withdrawal flow)</li>
 *   <li>Verifying correct data flow between components</li>
 *   <li>Testing configuration-driven behavior across subsystems</li>
 * </ul>
 *
 * @see io.github.xmljim.retirement.domain.calculator.impl Unit tests for calculators
 */
package io.github.xmljim.retirement.domain.integration;
