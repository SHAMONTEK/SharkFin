package com.example.sharkfin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

data class CalendarNote(
    val id: String = "",
    val text: String = "",
    val date: Long = 0L,
    val createdAt: Long = 0L
)

@Composable
fun CalendarScreen(
    uid: String,
    db: FirebaseFirestore,
    bills: List<Bill>
) {
    var calendarNotes by remember { mutableStateOf(listOf<CalendarNote>()) }
    val today = remember { Calendar.getInstance() }
    var selectedDate by remember { mutableStateOf(today.timeInMillis) }
    
    var viewMonth by remember { mutableIntStateOf(today.get(Calendar.MONTH)) }
    var viewYear by remember { mutableIntStateOf(today.get(Calendar.YEAR)) }

    // Listen for AI Coach Notes
    LaunchedEffect(uid) {
        db.collection("users").document(uid).collection("calendarNotes")
            .addSnapshotListener { snapshot, _ ->
                calendarNotes = snapshot?.documents?.mapNotNull { doc ->
                    CalendarNote(
                        id = doc.id,
                        text = doc.getString("text") ?: "",
                        date = doc.getLong("date") ?: 0L,
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                } ?: emptyList()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SharkBlack)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(56.dp))
        Text("Financial Calendar", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Shark's memory and your bills", color = SharkMuted, fontSize = 13.sp)

        Spacer(Modifier.height(24.dp))

        // Reusing central BillCalendar
        BillCalendar(
            bills = bills,
            viewMonth = viewMonth,
            viewYear = viewYear,
            today = today,
            onPrevMonth = { 
                if (viewMonth == 0) { viewMonth = 11; viewYear-- } 
                else viewMonth-- 
            },
            onNextMonth = { 
                if (viewMonth == 11) { viewMonth = 0; viewYear++ } 
                else viewMonth++ 
            },
            onDayClick = { day ->
                val cal = Calendar.getInstance().apply { 
                    set(viewYear, viewMonth, day)
                }
                selectedDate = cal.timeInMillis
            }
        )

        Spacer(Modifier.height(24.dp))

        val displayDate = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date(selectedDate))
        Text(displayDate.uppercase(), color = SharkGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        
        Spacer(Modifier.height(12.dp))

        val dayEvents = getEventsForDate(selectedDate, bills, calendarNotes)
        
        if (dayEvents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Clear day. No notes or bills.", color = SharkMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(dayEvents) { event ->
                    CalendarEventRow(event)
                }
            }
        }
    }
}

sealed class CalendarEvent {
    data class BillEvent(val bill: Bill) : CalendarEvent()
    data class NoteEvent(val note: CalendarNote) : CalendarEvent()
}

@Composable
fun CalendarEventRow(event: CalendarEvent) {
    val title: String
    val sub: String
    val color: Color
    val icon: androidx.compose.ui.graphics.vector.ImageVector

    when(event) {
        is CalendarEvent.BillEvent -> {
            title = event.bill.name
            sub = "Bill Payment"
            color = SharkAmber
            icon = Icons.Default.Receipt
        }
        is CalendarEvent.NoteEvent -> {
            title = "Note"
            sub = event.note.text
            color = SharkGreen
            icon = Icons.Default.ChatBubble
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = SharkMuted, fontSize = 12.sp)
        }
        if (event is CalendarEvent.BillEvent) {
            Text("$${event.bill.amount}", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

private fun getEventsForDate(timestamp: Long, bills: List<Bill>, notes: List<CalendarNote>): List<CalendarEvent> {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = cal.get(Calendar.MONTH)
    val year = cal.get(Calendar.YEAR)

    val events = mutableListOf<CalendarEvent>()
    
    // Add bills for this day
    bills.filter { it.dayOfMonth == day }.forEach { 
        events.add(CalendarEvent.BillEvent(it))
    }

    // Add notes for this day
    notes.filter { 
        val noteCal = Calendar.getInstance().apply { timeInMillis = it.date }
        noteCal.get(Calendar.DAY_OF_MONTH) == day &&
        noteCal.get(Calendar.MONTH) == month &&
        noteCal.get(Calendar.YEAR) == year
    }.forEach {
        events.add(CalendarEvent.NoteEvent(it))
    }

    return events
}
