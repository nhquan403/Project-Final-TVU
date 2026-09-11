package com.tvh.homestay.booking.repository;

import com.tvh.homestay.booking.entity.BookingNote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Ghi chú nội bộ trên đơn. */
public interface BookingNoteRepository extends JpaRepository<BookingNote, Long> {

    /**
     * Nạp kèm người viết bằng {@code join fetch}.
     *
     * <p>{@code open-in-view} đã tắt, nên đọc {@code note.getAuthor().getFullName()}
     * sau khi transaction đóng sẽ ném {@code LazyInitializationException}.
     */
    @Query("""
            select n from BookingNote n
            left join fetch n.author
            where n.booking.id = :bookingId
            order by n.id
            """)
    List<BookingNote> findWithAuthorByBookingId(Long bookingId);
}
