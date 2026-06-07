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
 * Installs Puppeteer-style P selector support into the shared injected registry.
 *
 * @param {typeof globalThis} globalThis Browser global object that owns the injected registry.
 */
(function installPQuerySelector(globalThis) {
  const injected = globalThis.__lanciaInjected || (globalThis.__lanciaInjected = {});
  const IDENT_TOKEN_START = /[-\w\P{ASCII}*]/u;

  /**
   * Checks whether a node supports selector queries.
   *
   * @param {Node|null|undefined} node Candidate node.
   * @returns {boolean} True when the node exposes querySelectorAll.
   */
  const isQueryableNode = node => node && 'querySelectorAll' in node;

  /**
   * Maps each item from an async iterable into another iterable and flattens the result.
   *
   * @param {AsyncIterable|Iterable} iterable Source iterable.
   * @param {Function} mapper Mapping function that returns an iterable.
   * @yields {*} Flattened mapped item.
   */
  async function* flatMap(iterable, mapper) {
    for await (const item of iterable) {
      yield* mapper(item);
    }
  }

  /**
   * Fallback deep-combinator implementation that yields the direct shadow root.
   *
   * @param {Element|ShadowRoot} root Search root.
   * @yields {ShadowRoot} Direct shadow root.
   */
  async function* fallbackPierce(root) {
    if (root && root.shadowRoot) {
      yield root.shadowRoot;
    }
  }

  /**
   * Fallback deep-combinator implementation that yields all descendant shadow roots.
   *
   * @param {Element|ShadowRoot} root Search root.
   * @yields {ShadowRoot} Descendant shadow root.
   */
  async function* fallbackPierceAll(root) {
    if (!isQueryableNode(root)) {
      return;
    }
    for (const element of root.querySelectorAll('*')) {
      if (element.shadowRoot) {
        yield element.shadowRoot;
        yield* fallbackPierceAll(element.shadowRoot);
      }
    }
  }

  /**
   * Fallback text selector implementation used when the text module is unavailable.
   *
   * @param {Element|ShadowRoot} root Search root.
   * @param {string} selector Text selector value.
   * @yields {Element} Matching element.
   */
  async function* fallbackTextQuerySelectorAll(root, selector) {
    if (!isQueryableNode(root)) {
      return;
    }
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_ELEMENT);
    let node = walker.currentNode;
    while (node) {
      if ((node.textContent || '').includes(selector)) {
        yield node;
      }
      node = walker.nextNode();
    }
  }

  /**
   * Fallback XPath selector implementation used when the XPath module is unavailable.
   *
   * @param {Node} root Search root.
   * @param {string} selector XPath selector expression.
   * @yields {Node} Matching node.
   */
  async function* fallbackXPathQuerySelectorAll(root, selector) {
    const doc = root.ownerDocument || document;
    const iterator = doc.evaluate(selector, root, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE);
    const items = [];
    let item;
    while ((item = iterator.iterateNext())) {
      items.push(item);
    }
    for (let i = 0; i < items.length; i++) {
      item = items[i];
      yield item;
      items[i] = null;
    }
  }

  /**
   * Executes a parsed P selector against a starting element.
   */
  class PQueryEngine {
    /**
     * Creates a query engine for one parsed complex selector.
     *
     * @param {Element|ShadowRoot|Document} element Initial search root.
     * @param {Array} complexSelector Parsed complex selector sequence.
     */
    constructor(element, complexSelector) {
      this.elements = [element];
      this.complexSelector = complexSelector.slice();
      this.compoundSelector = [];
      this.selector = undefined;
      this.next();
    }

    /**
     * Runs the parsed selector sequence and updates the current element stream.
     *
     * @returns {Promise<void>} Completion promise.
     */
    async run() {
      if (typeof this.selector === 'string' && this.selector.trimStart() === ':scope') {
        this.next();
      }
      for (; this.selector !== undefined; this.next()) {
        const selector = this.selector;
        if (typeof selector === 'string') {
          this.runCss(selector);
        } else {
          this.runPseudo(selector);
        }
      }
    }

    /**
     * Applies a CSS selector segment to the current element stream.
     *
     * @param {string} selector CSS selector segment.
     */
    runCss(selector) {
      if (selector[0] && IDENT_TOKEN_START.test(selector[0])) {
        this.elements = flatMap(this.elements, async function* (element) {
          if (isQueryableNode(element)) {
            yield* element.querySelectorAll(selector);
          }
        });
        return;
      }
      this.elements = flatMap(this.elements, async function* (element) {
        if (!element.parentElement) {
          if (isQueryableNode(element)) {
            yield* element.querySelectorAll(selector);
          }
          return;
        }
        let index = 0;
        for (const child of element.parentElement.children) {
          ++index;
          if (child === element) {
            break;
          }
        }
        yield* element.parentElement.querySelectorAll(`:scope>:nth-child(${index})${selector}`);
      });
    }

    /**
     * Applies a non-CSS pseudo selector segment to the current element stream.
     *
     * @param {{name: string, value: string}} selector Parsed pseudo selector segment.
     */
    runPseudo(selector) {
      this.elements = flatMap(this.elements, async function* (element) {
        switch (selector.name) {
          case 'text':
            yield* (injected.textQuerySelectorAll || fallbackTextQuerySelectorAll)(element, selector.value);
            break;
          case 'xpath':
            yield* (injected.xpathQuerySelectorAll || fallbackXPathQuerySelectorAll)(element, selector.value);
            break;
          case 'aria':
            yield* (injected.ariaQuerySelectorAll || (async function* () {}))(element, selector.value);
            break;
          case 'pierce':
            yield* (injected.pierceQuerySelectorAll || (async function* () {}))(element, selector.value);
            break;
          default: {
            const querySelector = injected.customQuerySelectors && injected.customQuerySelectors.get(selector.name);
            if (!querySelector) {
              throw new Error(`Unknown selector type: ${selector.name}`);
            }
            yield* querySelector.querySelectorAll(element, selector.value);
          }
        }
      });
    }

    /**
     * Advances the engine to the next selector segment or combinator.
     */
    next() {
      if (this.compoundSelector.length !== 0) {
        this.selector = this.compoundSelector.shift();
        return;
      }
      if (this.complexSelector.length === 0) {
        this.selector = undefined;
        return;
      }
      const selector = this.complexSelector.shift();
      switch (selector) {
        case '>>>>':
          this.elements = flatMap(this.elements, injected.pierce || fallbackPierce);
          this.next();
          break;
        case '>>>':
          this.elements = flatMap(this.elements, injected.pierceAll || fallbackPierceAll);
          this.next();
          break;
        default:
          this.compoundSelector = selector.slice();
          this.next();
      }
    }
  }

  /**
   * Calculates stable DOM depths for result ordering.
   */
  class DepthCalculator {
    /**
     * Creates an empty depth cache.
     */
    constructor() {
      this.cache = new WeakMap();
    }

    /**
     * Calculates a comparable ancestry depth path for a node.
     *
     * @param {Node|null} node Node to calculate.
     * @param {number[]} [depth=[]] Existing suffix depth.
     * @returns {number[]} Comparable depth path.
     */
    calculate(node, depth = []) {
      if (node === null) {
        return depth;
      }
      if (node instanceof ShadowRoot) {
        node = node.host;
      }
      const cachedDepth = this.cache.get(node);
      if (cachedDepth) {
        return [...cachedDepth, ...depth];
      }
      let index = 0;
      for (let prevSibling = node.previousSibling; prevSibling; prevSibling = prevSibling.previousSibling) {
        ++index;
      }
      const value = this.calculate(node.parentNode, [index]);
      this.cache.set(node, value);
      return [...value, ...depth];
    }
  }

  /**
   * Compares two DOM depth paths in document order.
   *
   * @param {number[]} a First depth path.
   * @param {number[]} b Second depth path.
   * @returns {number} Negative, zero, or positive comparison result.
   */
  function compareDepths(a, b) {
    if (a.length + b.length === 0) {
      return 0;
    }
    const [i = -1, ...otherA] = a;
    const [j = -1, ...otherB] = b;
    if (i === j) {
      return compareDepths(otherA, otherB);
    }
    return i < j ? -1 : 1;
  }

  /**
   * Deduplicates and yields query results in DOM order.
   *
   * @param {AsyncIterable|Iterable} elements Query result stream.
   * @yields {Element|Node} Result ordered by DOM position.
   */
  async function* domSort(elements) {
    const results = new Set();
    for await (const element of elements) {
      results.add(element);
    }
    const calculator = new DepthCalculator();
    yield* [...results.values()]
      .map(result => [result, calculator.calculate(result)])
      .sort(([, a], [, b]) => compareDepths(a, b))
      .map(([result]) => result);
  }

  /**
   * Queries all nodes matching a serialized P selector.
   *
   * @param {Element|ShadowRoot|Document} root Search root.
   * @param {string} selector Serialized parsed selector.
   * @returns {AsyncIterable<Element|Node>} Ordered result stream.
   */
  function pQuerySelectorAll(root, selector) {
    const selectors = JSON.parse(selector);
    if (
      selectors.some(parts => {
        let i = 0;
        return parts.some(part => {
          if (typeof part === 'string') {
            ++i;
          } else {
            i = 0;
          }
          return i > 1;
        });
      })
    ) {
      throw new Error('Multiple deep combinators found in sequence.');
    }

    return domSort(
      flatMap(selectors, selectorParts => {
        const query = new PQueryEngine(root, selectorParts);
        void query.run();
        return query.elements;
      })
    );
  }

  /**
   * Queries the first node matching a serialized P selector.
   *
   * @param {Element|ShadowRoot|Document} root Search root.
   * @param {string} selector Serialized parsed selector.
   * @returns {Promise<Element|Node|null>} First matching node, or null when no node matches.
   */
  async function pQuerySelector(root, selector) {
    for await (const element of pQuerySelectorAll(root, selector)) {
      return element;
    }
    return null;
  }

  injected.PQueryEngine = PQueryEngine;
  injected.DepthCalculator = DepthCalculator;
  injected.pQuerySelectorAll = pQuerySelectorAll;
  injected.pQuerySelector = pQuerySelector;
})(globalThis);
