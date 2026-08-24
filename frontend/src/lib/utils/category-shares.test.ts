import assert from 'node:assert/strict';
import test from 'node:test';

import { categoryShares } from './category-shares.ts';

test('allocates whole category shares that total exactly 100 percent', () => {
	assert.deepEqual(categoryShares(['1', '1', '1', '1', '1', '1']), [17, 17, 17, 17, 16, 16]);
	assert.equal(categoryShares(['9223372036854775807', '9223372036854775807']).reduce((sum, share) => sum + share, 0), 100);
});
