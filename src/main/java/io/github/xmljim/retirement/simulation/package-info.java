/**
 * Simulation Engine Layer.
 *
 * <p>Contains the core simulation logic, calculators, and strategies
 * for modeling retirement scenarios over time.
 *
 * <p>This layer depends on the domain layer and provides the business
 * logic for running retirement simulations.
 *
 * <h2>Sub-packages</h2>
 * <ul>
 *   <li>{@code engine} - Core simulation runner and monthly loop</li>
 *   <li>{@code calculator} - Financial calculation services</li>
 *   <li>{@code strategy} - Distribution strategy implementations</li>
 *   <li>{@code income} - Income source modeling</li>
 *   <li>{@code expense} - Expense and budget modeling</li>
 *   <li>{@code rules} - IRS rules and contribution limits</li>
 *   <li>{@code result} - Simulation result models</li>
 * </ul>
 */
package io.github.xmljim.retirement.simulation;
