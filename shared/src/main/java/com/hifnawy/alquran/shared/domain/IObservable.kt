package com.hifnawy.alquran.shared.domain

/**
 * Represents an observer in the observer design pattern.
 *
 * This is a marker interface for components, typically within the UI layer like
 * Activities or Fragments, that wish to be notified of changes.
 *
 * Implementers of this interface are expected to register themselves with a
 * corresponding subject to receive updates. When the state of the subject changes,
 * it will notify all its registered observers. This pattern promotes loose coupling
 * between the data/logic layer (Subject) and the presentation layer (Observer).
 *
 * This specific interface is a marker, meaning it has no methods. Sub-interfaces
 * should define the specific `update` methods relevant to the data they observe.
 */
interface IObservable
