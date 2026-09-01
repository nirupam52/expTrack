export function categoryShares(amounts: string[]) {
	const total = amounts.reduce((sum, amount) => sum + BigInt(amount), 0n);
	if (total === 0n) return amounts.map(() => 0);
	const shares = amounts.map((amount, index) => {
		const value = BigInt(amount) * 100n;
		return { index, share: Number(value / total), remainder: value % total };
	});
	const remaining = 100 - shares.reduce((sum, item) => sum + item.share, 0);
	for (const item of [...shares].sort((left, right) => right.remainder === left.remainder ? left.index - right.index : right.remainder > left.remainder ? 1 : -1).slice(0, remaining)) {
		item.share += 1;
	}
	return shares.map((item) => item.share);
}
