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
 ~ Copyright 2023 Google Inc.                                                ~
 ~ SPDX-License-Identifier: Apache-2.0                                       ~
 ~                                                                           ~
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
*/

/**
 * Installs custom query selector support into the shared injected registry.
 *
 * @param {typeof globalThis} globalThis Browser global object that owns the injected registry.
 */
(function installCustomQuerySelector(globalThis) {
  const injected = globalThis.__lanciaInjected || (globalThis.__lanciaInjected = {});

  /**
   * Stores user-defined query handlers and normalizes one/all query APIs.
   */
  class CustomQuerySelectorRegistry {
    /**
     * Creates a registry backed by the global custom handler map.
     */
    constructor() {
      this.selectors = globalThis.__lanciaCustomQueryHandlers || new Map();
      globalThis.__lanciaCustomQueryHandlers = this.selectors;
    }

    /**
     * Registers a custom query handler.
     *
     * @param {string} name Custom selector engine name.
     * @param {{queryOne?: Function, queryAll?: Function}} handler Custom query handler.
     */
    register(name, handler) {
      if (!handler.queryOne && handler.queryAll) {
        const querySelectorAll = handler.queryAll;
        /**
         * Adapts a query-all handler into a query-one handler.
         *
         * @param {Node} node Search root.
         * @param {string} selector Selector expression.
         * @returns {Node|null} First matching node, or null when no node matches.
         */
        handler.queryOne = (node, selector) => {
          for (const result of querySelectorAll(node, selector)) {
            return result;
          }
          return null;
        };
      } else if (handler.queryOne && !handler.queryAll) {
        const querySelector = handler.queryOne;
        /**
         * Adapts a query-one handler into a query-all handler.
         *
         * @param {Node} node Search root.
         * @param {string} selector Selector expression.
         * @returns {Node[]} Matching nodes.
         */
        handler.queryAll = (node, selector) => {
          const result = querySelector(node, selector);
          return result ? [result] : [];
        };
      } else if (!handler.queryOne || !handler.queryAll) {
        throw new Error('At least one query method must be defined.');
      }

      this.selectors.set(name, {
        querySelector: handler.queryOne,
        querySelectorAll: handler.queryAll,
        queryOne: handler.queryOne,
        queryAll: handler.queryAll,
      });
    }

    /**
     * Removes a custom query handler by name.
     *
     * @param {string} name Custom selector engine name.
     */
    unregister(name) {
      this.selectors.delete(name);
    }

    /**
     * Returns a registered custom query handler.
     *
     * @param {string} name Custom selector engine name.
     * @returns {object|undefined} Registered handler, or undefined when absent.
     */
    get(name) {
      return this.selectors.get(name);
    }

    /**
     * Clears every registered custom query handler.
     */
    clear() {
      this.selectors.clear();
    }
  }

  injected.CustomQuerySelectorRegistry = CustomQuerySelectorRegistry;
  injected.customQuerySelectors = injected.customQuerySelectors || new CustomQuerySelectorRegistry();
})(globalThis);
