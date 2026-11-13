import assert from 'node:assert/strict'
import test from 'node:test'
import { CATALOG_PRICE_MAX, catalogPriceRanges, formatCatalogPriceInput, normalizeCatalogPriceInput, validateCatalogPrices } from './productModel.js'

test('VND inputs accept digits and Vietnamese thousands separators without losing value', () => {
  for (const [input, expected] of [
    ['', ''], ['0', '0'], ['1', '1'], ['000001', '1'],
    ['100000', '100000'], [' 100.000 ', '100000'], ['9.999.999.999', '9999999999'],
  ]) {
    assert.equal(normalizeCatalogPriceInput(input), expected)
  }
  assert.equal(formatCatalogPriceInput('1000000'), '1.000.000')
  assert.equal(formatCatalogPriceInput('0'), '0')
  assert.equal(formatCatalogPriceInput(''), '')
  assert.equal(formatCatalogPriceInput('9007199254740993'), '9.007.199.254.740.993')
})

test('invalid monetary notation is reported without silently changing its amount', () => {
  for (const input of ['-100', '+100', '1e3', '12.34', '100,50', '100000.50', '1,000.00', 'abc', '100.000 ₫']) {
    const normalized = normalizeCatalogPriceInput(input)
    assert.equal(normalized, input)
    assert.ok(validateCatalogPrices({ minPrice: normalized, maxPrice: '' }).minPrice, input)
  }
})

test('catalog ranges allow optional bounds, zero, single dong amounts and equal bounds', () => {
  for (const prices of [
    { minPrice: '', maxPrice: '' },
    { minPrice: '0', maxPrice: '' },
    { minPrice: '', maxPrice: '0' },
    { minPrice: '1', maxPrice: '999' },
    { minPrice: '100001', maxPrice: '100001' },
    { minPrice: String(CATALOG_PRICE_MAX), maxPrice: String(CATALOG_PRICE_MAX) },
  ]) {
    assert.deepEqual(validateCatalogPrices(prices), {}, JSON.stringify(prices))
  }
})

test('catalog ranges reject excessive budgets and reversed bounds on the correct fields', () => {
  assert.ok(validateCatalogPrices({ minPrice: String(CATALOG_PRICE_MAX + 1), maxPrice: '' }).minPrice)
  assert.ok(validateCatalogPrices({ minPrice: '1', maxPrice: '900000000000' }).maxPrice)
  assert.ok(validateCatalogPrices({ minPrice: '100001', maxPrice: '100000' }).maxPrice)
  assert.ok(validateCatalogPrices({ minPrice: '1', maxPrice: '0' }).maxPrice)
  assert.ok(validateCatalogPrices({ minPrice: '9'.repeat(100), maxPrice: '' }).minPrice)
})

test('open ended price presets exclude the threshold and include prices one cent away', () => {
  const under = catalogPriceRanges.find(range => range.id === 'under-150k')
  const over = catalogPriceRanges.find(range => range.id === 'over-5m')
  assert.ok(Number(under.maxPrice) < 150000)
  assert.ok(149999.99 <= Number(under.maxPrice))
  assert.ok(Number(over.minPrice) > 5000000)
  assert.ok(5000000.01 >= Number(over.minPrice))
  assert.deepEqual(catalogPriceRanges.find(range => range.id === 'all'), {
    id: 'all', label: 'Tất cả mức giá', minPrice: '', maxPrice: '',
  })
})
