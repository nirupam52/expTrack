<script lang="ts">
	import type { AccountPasswords, Session } from '$lib/api/types';
	import AccountSettings from '$lib/components/AccountSettings.svelte';

	let { session, savingCurrency, savingPassword, currencyError, passwordError, onDirty, onSaveCurrency, onChangePassword }: {
		session: Session;
		savingCurrency: boolean;
		savingPassword: boolean;
		currencyError: string;
		passwordError: string;
		onDirty: (dirty: boolean) => void;
		onSaveCurrency: (currency: string) => Promise<boolean>;
		onChangePassword: (passwords: AccountPasswords) => Promise<boolean>;
	} = $props();
</script>

<AccountSettings
	email={session.email}
	createdAt={session.createdAt}
	defaultCurrency={session.defaultCurrency}
	{savingCurrency}
	{savingPassword}
	{currencyError}
	{passwordError}
	onDirty={onDirty}
	onSaveCurrency={onSaveCurrency}
	onChangePassword={onChangePassword}
/>
