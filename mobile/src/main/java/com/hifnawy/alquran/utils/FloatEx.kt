package com.hifnawy.alquran.utils

import com.hifnawy.alquran.utils.FloatEx.isApproximately
import kotlin.math.abs

/**
 * An extension object for the [Float] class, providing utility functions
 * for floating-point comparisons.
 *
 * This object enables a more readable, infix-style syntax for checking if two
 * floats are approximately equal within a certain tolerance (epsilon).
 *
 * Example usage:
 * ```kotlin
 * if (0.1f + 0.2f isApproximately 0.3f within 0.00001f) {
 *     // This will be true, accounting for floating-point inaccuracies.
 * }
 * ```
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see ApproximateComparison
 * @see isApproximately
 */
object FloatEx {

    /**
     * A helper class that holds two floats for an approximate comparison.
     *
     * This class is typically not instantiated directly. It is created as an
     * intermediate object by the [isApproximately] infix function to allow for
     * a fluent, natural-language-like comparison syntax using the [within] infix function.
     *
     * The expression:
     *
     * ```kotlin
     * 0.1f + 0.2f isApproximately 0.3f within 0.00001f
     * ```
     *
     * is made possible by this class.
     *
     * @property one [Float] The first float in the comparison.
     * @property other [Float] The second float to compare against the first.
     *
     * @author AbdElMoniem ElHifnawy
     *
     * @see FloatEx.isApproximately
     * @see within
     */
    data class ApproximateComparison(val one: Float, val other: Float) {

        /**
         * Completes the approximate comparison, checking if the absolute difference
         * between the two floats ([one] and [other]) is less than the specified tolerance.
         *
         * This function serves as the final part of the [isApproximately] ... [within] ...
         * chain.
         *
         * Example:
         * ```kotlin
         * // The 'within' part of the expression executes this function.
         * 0.1f + 0.2f isApproximately 0.3f within 0.00001f
         * ```
         *
         * @param epsilon The maximum allowed difference (tolerance) for the two floats
         *   to be considered equal. Must be a non-negative value.
         * @return `true` if `abs(one - other) < epsilon`, `false` otherwise.
         */
        infix fun within(epsilon: Float) = abs(one - other) < epsilon
    }

    /**
     * Initiates an approximate comparison between two [Float] values, enabling a fluent,
     * natural-language-like syntax.
     *
     * This infix function is the first part of a two-step comparison. It creates an
     * [ApproximateComparison] object containing the two floats. The comparison is then
     * completed by using the [ApproximateComparison.within] infix function to specify the
     * tolerance (epsilon).
     *
     * This approach is necessary because Kotlin's infix functions can only take one parameter.
     *
     * Example usage:
     *
     * ```kotlin
     * // The full expression is chained like this:
     * val a = 0.1f + 0.2f
     * val b = 0.3f
     * val tolerance = 0.000001f
     *
     * if (a isApproximately b within tolerance) {
     *     println("a is approximately equal to b")
     * }
     * ```
     *
     * @receiver [Float] The first float in the comparison.
     *
     * @param other [Float] The second float to compare against.
     *
     * @return An [ApproximateComparison] object, ready for the [ApproximateComparison.within] check.
     *
     * @author AbdElMoniem ElHifnawy
     *
     * @see ApproximateComparison
     * @see ApproximateComparison.within
     *
     */
    infix fun Float.isApproximately(other: Float) = ApproximateComparison(this, other)
}
