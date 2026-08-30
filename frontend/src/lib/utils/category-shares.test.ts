import { expect, test } from 'vitest';

import { categoryShares } from './category-shares.ts';

test('allocates whole category shares that total exactly 100 percent', () => {
	expect(categoryShares(['1', '1', '1', '1', '1', '1'])).toEqual([17, 17, 17, 17, 16, 16]);
	expect(categoryShares(['9223372036854775807', '9223372036854775807']).reduce((sum, share) => sum + share, 0)).toBe(100);
});
