package com.exptrack.expense.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.exptrack.expense.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Integer> {

	@Query(value = """
			SELECT e.* FROM expenses e
			WHERE e.user_id = :#{#history.userId}
			AND (CAST(:#{#history.text} AS text) IS NULL OR to_tsvector(
				'simple'::regconfig,
				coalesce(e.title, '') || ' ' || coalesce(e.note, '')
			) @@ websearch_to_tsquery(
				'simple'::regconfig,
				:#{#history.text}
			))
			AND (CAST(:#{#history.categoryId} AS integer) IS NULL OR e.category_id = :#{#history.categoryId})
			AND (CAST(:#{#history.currency} AS text) IS NULL OR e.currency = :#{#history.currency})
			AND (CAST(:#{#history.fromDate} AS date) IS NULL OR e.expense_date >= :#{#history.fromDate})
			AND (CAST(:#{#history.toDate} AS date) IS NULL OR e.expense_date <= :#{#history.toDate})
			AND (CAST(:#{#history.cursorDate} AS date) IS NULL OR e.expense_date < :#{#history.cursorDate}
				OR (e.expense_date = :#{#history.cursorDate} AND e.id < :#{#history.cursorId}))
			ORDER BY e.expense_date DESC, e.id DESC
			LIMIT :#{#history.limit}
			""", nativeQuery = true)
	List<Expense> findHistory(@Param("history") ExpenseHistoryQuery history);

	@Query(value = """
			SELECT e.currency AS currency, e.category_id AS categoryId, e.amount_minor AS amountMinor
			FROM expenses e
			WHERE e.user_id = :userId AND e.expense_date >= :fromDate AND e.expense_date < :toDate
			ORDER BY e.currency, e.category_id
			""", nativeQuery = true)
	List<DashboardExpenseAmount> findDashboardExpenseAmounts(
			@Param("userId") Integer userId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

	List<Expense> findTop5ByUserIdOrderByExpenseDateDescIdDesc(Integer userId);

	Optional<Expense> findByIdAndUserId(Integer id, Integer userId);
}
