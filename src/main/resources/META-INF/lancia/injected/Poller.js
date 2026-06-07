/*
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
 ~                                                                           ~
 ~ Copyright (c) 2015-2026 miaixz.org and other contributors.                ~
 ~                                                                           ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");           ~
 ~ you may not use this file except in compliance with the License.          ~
 ~ You may obtain a copy of the License at                                   ~
 ~                                                                           ~
 ~      https://www.apache.org/licenses/LICENSE-2.0                          ~
 ~                                                                           ~
 ~ Unless required by applicable law or agreed to in writing, software       ~
 ~ distributed under the License is distributed on an "AS IS" BASIS,         ~
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  ~
 ~ See the License for the specific language governing permissions and       ~
 ~ limitations under the License.                                            ~
 ~                                                                           ~
 ~ Derived from Puppeteer.                                                   ~
 ~ Copyright 2022 Google Inc.                                                ~
 ~ SPDX-License-Identifier: Apache-2.0                                       ~
 ~                                                                           ~
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
*/

/**
 * Installs polling primitives used by wait-for-selector evaluation.
 *
 * @param {typeof globalThis} globalThis Browser global object that owns the injected registry.
 */
(function installPoller(globalThis) {
  const injected = globalThis.__lanciaInjected || (globalThis.__lanciaInjected = {});

  /**
   * Small promise holder that can be resolved or rejected exactly once.
   */
  class Deferred {
    /**
     * Creates a pending deferred promise.
     */
    constructor() {
      this.done = false;
      this.promise = new Promise((resolve, reject) => {
        this.resolveValue = resolve;
        this.rejectValue = reject;
      });
    }

    /**
     * Resolves the deferred promise when it is still pending.
     *
     * @param {*} value Resolution value.
     */
    resolve(value) {
      if (!this.done) {
        this.done = true;
        this.resolveValue(value);
      }
    }

    /**
     * Rejects the deferred promise when it is still pending.
     *
     * @param {Error} error Rejection error.
     */
    reject(error) {
      if (!this.done) {
        this.done = true;
        this.rejectValue(error);
      }
    }

    /**
     * Checks whether this deferred promise has already completed.
     *
     * @returns {boolean} True when the promise has completed.
     */
    finished() {
      return this.done;
    }

    /**
     * Returns the underlying promise.
     *
     * @returns {Promise<*>} Deferred promise.
     */
    valueOrThrow() {
      return this.promise;
    }
  }

  /**
   * Ensures that a poller has been started before stop or result access.
   *
   * @param {Deferred|undefined} deferred Poller deferred state.
   */
  function assertStarted(deferred) {
    if (!deferred) {
      throw new Error('Polling never started.');
    }
  }

  /**
   * Polls by re-running the predicate after DOM mutations.
   */
  class MutationPoller {
    /**
     * Creates a mutation-driven poller.
     *
     * @param {Function} fn Predicate evaluated until it returns a truthy value.
     * @param {Node} root DOM root observed for mutations.
     */
    constructor(fn, root) {
      this.fn = fn;
      this.root = root;
      this.observer = undefined;
      this.deferred = undefined;
    }

    /**
     * Starts mutation observation and resolves when the predicate succeeds.
     *
     * @returns {Promise<void>} Completion promise.
     */
    async start() {
      const deferred = (this.deferred = new Deferred());
      const result = await this.fn();
      if (result) {
        deferred.resolve(result);
        return;
      }

      this.observer = new MutationObserver(async () => {
        const result = await this.fn();
        if (!result) {
          return;
        }
        deferred.resolve(result);
        await this.stop();
      });
      this.observer.observe(this.root, {
        childList: true,
        subtree: true,
        attributes: true,
      });
    }

    /**
     * Stops observation and rejects the pending result when unresolved.
     *
     * @returns {Promise<void>} Completion promise.
     */
    async stop() {
      assertStarted(this.deferred);
      if (!this.deferred.finished()) {
        this.deferred.reject(new Error('Polling stopped'));
      }
      if (this.observer) {
        this.observer.disconnect();
        this.observer = undefined;
      }
    }

    /**
     * Returns the promise resolved by the first successful predicate value.
     *
     * @returns {Promise<*>} Polling result promise.
     */
    result() {
      assertStarted(this.deferred);
      return this.deferred.valueOrThrow();
    }
  }

  /**
   * Polls by re-running the predicate on animation frames.
   */
  class RAFPoller {
    /**
     * Creates a requestAnimationFrame-driven poller.
     *
     * @param {Function} fn Predicate evaluated until it returns a truthy value.
     */
    constructor(fn) {
      this.fn = fn;
      this.deferred = undefined;
    }

    /**
     * Starts animation-frame polling and resolves when the predicate succeeds.
     *
     * @returns {Promise<void>} Completion promise.
     */
    async start() {
      const deferred = (this.deferred = new Deferred());
      const result = await this.fn();
      if (result) {
        deferred.resolve(result);
        return;
      }

      /**
       * Executes one animation-frame polling cycle.
       *
       * @returns {Promise<void>} Completion promise for the current cycle.
       */
      const poll = async () => {
        if (deferred.finished()) {
          return;
        }
        const result = await this.fn();
        if (!result) {
          window.requestAnimationFrame(poll);
          return;
        }
        deferred.resolve(result);
        await this.stop();
      };
      window.requestAnimationFrame(poll);
    }

    /**
     * Stops animation-frame polling and rejects the pending result when unresolved.
     *
     * @returns {Promise<void>} Completion promise.
     */
    async stop() {
      assertStarted(this.deferred);
      if (!this.deferred.finished()) {
        this.deferred.reject(new Error('Polling stopped'));
      }
    }

    /**
     * Returns the promise resolved by the first successful predicate value.
     *
     * @returns {Promise<*>} Polling result promise.
     */
    result() {
      assertStarted(this.deferred);
      return this.deferred.valueOrThrow();
    }
  }

  /**
   * Polls by re-running the predicate on a fixed interval.
   */
  class IntervalPoller {
    /**
     * Creates an interval-driven poller.
     *
     * @param {Function} fn Predicate evaluated until it returns a truthy value.
     * @param {number} ms Polling interval in milliseconds.
     */
    constructor(fn, ms) {
      this.fn = fn;
      this.ms = ms;
      this.interval = undefined;
      this.deferred = undefined;
    }

    /**
     * Starts interval polling and resolves when the predicate succeeds.
     *
     * @returns {Promise<void>} Completion promise.
     */
    async start() {
      const deferred = (this.deferred = new Deferred());
      const result = await this.fn();
      if (result) {
        deferred.resolve(result);
        return;
      }

      this.interval = setInterval(async () => {
        const result = await this.fn();
        if (!result) {
          return;
        }
        deferred.resolve(result);
        await this.stop();
      }, this.ms);
    }

    /**
     * Stops interval polling and rejects the pending result when unresolved.
     *
     * @returns {Promise<void>} Completion promise.
     */
    async stop() {
      assertStarted(this.deferred);
      if (!this.deferred.finished()) {
        this.deferred.reject(new Error('Polling stopped'));
      }
      if (this.interval) {
        clearInterval(this.interval);
        this.interval = undefined;
      }
    }

    /**
     * Returns the promise resolved by the first successful predicate value.
     *
     * @returns {Promise<*>} Polling result promise.
     */
    result() {
      assertStarted(this.deferred);
      return this.deferred.valueOrThrow();
    }
  }

  injected.Deferred = injected.Deferred || Deferred;
  injected.MutationPoller = MutationPoller;
  injected.RAFPoller = RAFPoller;
  injected.IntervalPoller = IntervalPoller;
})(globalThis);
