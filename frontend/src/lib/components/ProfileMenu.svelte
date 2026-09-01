<script lang="ts">
	import { tick } from 'svelte';

	let { onAccountSettings, onSignOut, signOutError }: {
		onAccountSettings: () => void;
		onSignOut: () => void;
		signOutError: string;
	} = $props();

	let open = $state(false);
	let menuWrap = $state<HTMLElement | null>(null);
	let trigger = $state<HTMLButtonElement | null>(null);

	async function openMenu() {
		if (open) return;
		open = true;
		await tick();
		menuWrap?.querySelector<HTMLElement>('[role="menuitem"]')?.focus();
	}

	async function toggleMenu() {
		if (open) {
			closeMenu(true);
			return;
		}
		await openMenu();
	}

	function closeMenu(refocus = false) {
		open = false;
		if (refocus) trigger?.focus();
	}

	function triggerKeydown(event: KeyboardEvent) {
		if (event.key !== 'ArrowDown' && event.key !== 'ArrowUp') return;
		event.preventDefault();
		void openMenu();
	}

	function menuKeydown(event: KeyboardEvent) {
		const items = Array.from(menuWrap?.querySelectorAll<HTMLElement>('[role="menuitem"]') ?? []);
		const index = items.indexOf(document.activeElement as HTMLElement);
		if (event.key === 'ArrowDown') {
			event.preventDefault();
			items[(index + 1) % items.length]?.focus();
		} else if (event.key === 'ArrowUp') {
			event.preventDefault();
			items[(index - 1 + items.length) % items.length]?.focus();
		} else if (event.key === 'Escape') {
			event.preventDefault();
			closeMenu(true);
		} else if (event.key === 'Tab') {
			closeMenu();
		}
	}

	function chooseAccountSettings() {
		closeMenu();
		onAccountSettings();
	}

	function chooseSignOut() {
		closeMenu();
		onSignOut();
	}

	function handleWindowPointerDown(event: PointerEvent) {
		if (open && menuWrap && !menuWrap.contains(event.target as Node)) closeMenu();
	}
</script>

<svelte:window onpointerdown={handleWindowPointerDown} />

<div class="account-actions">
	<div class="profile-menu-wrap" bind:this={menuWrap} onfocusout={(event) => { if (!event.currentTarget.contains(event.relatedTarget as Node | null)) closeMenu(); }}>
		<button class="quiet" bind:this={trigger} aria-haspopup="menu" aria-expanded={open} onclick={() => void toggleMenu()} onkeydown={triggerKeydown}>Profile</button>
		{#if open}
			<div class="profile-menu" role="menu" tabindex="-1" aria-label="Profile" onkeydown={menuKeydown}>
				<button role="menuitem" onclick={chooseAccountSettings}>Account settings</button>
				<button role="menuitem" onclick={chooseSignOut}>Sign out</button>
			</div>
		{/if}
	</div>
	{#if signOutError}<p class="error" role="alert">{signOutError}</p>{/if}
</div>
