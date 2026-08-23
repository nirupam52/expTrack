package com.exptrack.expense.repository;

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
			AND (:#{#history.text} IS NULL OR e.id IN (
				SELECT rowid FROM expenses_search
				WHERE expenses_search MATCH :#{#history.text}
			))
			AND (:#{#history.categoryId} IS NULL OR e.category_id = :#{#history.categoryId})
			AND (:#{#history.fromDate} IS NULL OR e.expense_date >= :#{#history.fromDate})
			AND (:#{#history.toDate} IS NULL OR e.expense_date <= :#{#history.toDate})
			AND (:#{#history.cursorDate} IS NULL OR e.expense_date < :#{#history.cursorDate}
				OR (e.expense_date = :#{#history.cursorDate} AND e.id < :#{#history.cursorId}))
			ORDER BY e.expense_date DESC, e.id DESC
			LIMIT :#{#history.limit}
			""", nativeQuery = true)
	List<Expense> findHistory(@Param("history") ExpenseHistoryQuery history);

	Optional<Expense> findByIdAndUserId(Integer id, Integer userId);
}
