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
 ~ Copyright 2024 Google Inc.                                                ~
 ~ SPDX-License-Identifier: Apache-2.0                                       ~
 ~                                                                           ~
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
*/

/**
 * Installs the shared injected runtime bridge and base DOM primitives.
 *
 * @param {typeof globalThis} globalThis Browser global object that owns the injected registry.
 */
(function installInjected(globalThis) {
  const injected = globalThis.__lanciaInjected || (globalThis.__lanciaInjected = {});
  const HIDDEN_VISIBILITY_VALUES = ['hidden', 'collapse'];

  /**
   * Checks whether an element has an empty layout rectangle.
   *
   * @param {Element} element Element to inspect.
   * @returns {boolean} True when the element has no rendered width or height.
   */
  function isBoundingBoxEmpty(element) {
    const rect = element.getBoundingClientRect();
    return rect.width === 0 || rect.height === 0;
  }

  /**
   * Checks a node against an optional visibility expectation.
   *
   * @param {Node|null} node Candidate node.
   * @param {boolean|undefined} visible Expected visibility, or undefined to return the node.
   * @returns {Node|boolean} The node when it matches, false when it does not, or true for null-hidden checks.
   */
  function checkVisibility(node, visible) {
    if (!node) {
      return visible === false;
    }
    if (visible === undefined) {
      return node;
    }
    const element = node.nodeType === Node.TEXT_NODE ? node.parentElement : node;
    if (!element) {
      return false;
    }

    const style = window.getComputedStyle(element);
    const isVisible =
      style &&
      !HIDDEN_VISIBILITY_VALUES.includes(style.visibility) &&
      !isBoundingBoxEmpty(element);
    return visible === isVisible ? node : false;
  }

  /**
   * Checks whether a node has an open shadow root.
   *
   * @param {Node} node Candidate node.
   * @returns {boolean} True when the node exposes a shadow root.
   */
  function hasShadowRoot(node) {
    return 'shadowRoot' in node && node.shadowRoot instanceof ShadowRoot;
  }

  /**
   * Yields the effective traversal root, entering an open shadow root when present.
   *
   * @param {Node} root Traversal root.
   * @yields {Node|ShadowRoot} Effective traversal root.
   */
  function* pierce(root) {
    if (hasShadowRoot(root)) {
      yield root.shadowRoot;
    } else {
      yield root;
    }
  }

  /**
   * Yields a root and every open shadow root beneath it.
   *
   * @param {Node|ShadowRoot} root Traversal root.
   * @yields {Node|ShadowRoot} Traversable root.
   */
  function* pierceAll(root) {
    root = pierce(root).next().value;
    yield root;
    const walkers = [document.createTreeWalker(root, NodeFilter.SHOW_ELEMENT)];
    for (const walker of walkers) {
      let node;
      while ((node = walker.nextNode())) {
        if (!node.shadowRoot) {
          continue;
        }
        yield node.shadowRoot;
        walkers.push(document.createTreeWalker(node.shadowRoot, NodeFilter.SHOW_ELEMENT));
      }
    }
  }

  if (!injected.createFunction) {
    /**
     * Converts a serialized function expression into an executable function.
     *
     * Trust boundary: this executes caller-provided page function source with
     * new Function. The caller must ensure the source is trusted; the injected
     * runtime intentionally does not sanitize, rewrite, or filter function code.
     *
     * @param {string|Function} functionValue Function source or function instance.
     * @returns {Function} Executable function.
     */
    injected.createFunction = function createFunction(functionValue) {
      if (typeof functionValue === 'function') {
        return functionValue;
      }
      return new Function(`return (${functionValue})`)();
    };
  }

  injected.checkVisibility = checkVisibility;
  injected.pierce = pierce;
  injected.pierceAll = pierceAll;

  const names = [
    'ariaQuerySelector',
    'ariaQuerySelectorAll',
    'CustomQuerySelectorRegistry',
    'customQuerySelectors',
    'pierceQuerySelector',
    'pierceQuerySelectorAll',
    'PQueryEngine',
    'DepthCalculator',
    'pQuerySelector',
    'pQuerySelectorAll',
    'textQuerySelectorAll',
    'checkVisibility',
    'pierce',
    'pierceAll',
    'xpathQuerySelectorAll',
    'cssQuerySelector',
    'cssQuerySelectorAll',
    'Deferred',
    'createFunction',
    'createTextContent',
    'IntervalPoller',
    'isSuitableNodeForTextMatching',
    'MutationPoller',
    'RAFPoller',
  ];

  const Lancia = {};
  for (const name of names) {
    Object.defineProperty(Lancia, name, {
      enumerable: true,
      /**
       * Resolves the latest injected runtime value for the exported name.
       *
       * @returns {*} Runtime value from the injected registry.
       */
      get() {
        return injected[name];
      },
    });
  }

  injected.Lancia = Object.freeze(Lancia);
  globalThis.Lancia = injected.Lancia;
  globalThis.__lanciaRuntime = injected.Lancia;
})(globalThis);
