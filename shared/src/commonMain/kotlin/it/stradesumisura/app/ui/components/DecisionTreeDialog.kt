package it.stradesumisura.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.stradesumisura.app.domain.DecisionNode
import it.stradesumisura.app.domain.DecisionOutcome
import it.stradesumisura.app.domain.ToolItem

/** Punto in cui ci si trova nell'albero dopo le risposte date finora. */
private sealed interface Position {
    /** Domanda da porre, con il titolo della parte (per gli alberi composti) e le risposte già date. */
    data class Pending(
        val partTitle: String?,
        val question: DecisionNode.Question,
        val trail: List<String>,
        val partScores: List<Pair<String, Int>>,
    ) : Position

    /** Percorso completato. */
    data class Done(val outcome: DecisionOutcome, val trail: List<String>) : Position
}

private fun DecisionNode.position(path: List<Int>): Position {
    var cursor = 0
    val trail = mutableListOf<String>()
    val partScores = mutableListOf<Pair<String, Int>>()

    // Restituisce l'esito della parte percorsa oppure la domanda in sospeso.
    fun walk(node: DecisionNode, partTitle: String?): Any = when (node) {
        is DecisionNode.Result -> DecisionOutcome(node.score, node.label, emptyList())
        is DecisionNode.Question -> {
            val index = path.getOrNull(cursor)
            val answer = index?.let { node.answers.getOrNull(it) }
            if (answer == null) {
                Position.Pending(partTitle, node, trail.toList(), partScores.toList())
            } else {
                cursor++
                trail += answer.label
                walk(answer.next, partTitle)
            }
        }
        is DecisionNode.LowerOf -> {
            var pending: Position.Pending? = null
            val outcomes = mutableListOf<Pair<String, DecisionOutcome>>()
            for (part in node.parts) {
                val result = walk(part.root, part.title)
                if (result is Position.Pending) {
                    pending = result
                    break
                }
                val outcome = result as DecisionOutcome
                outcomes += part.title to outcome
                partScores += part.title to outcome.score
            }
            pending ?: outcomes.minBy { it.second.score }.let { lowest ->
                DecisionOutcome(lowest.second.score, lowest.second.label, outcomes.map { it.first to it.second.score })
            }
        }
    }
    return when (val result = walk(this, null)) {
        is Position.Pending -> result
        else -> Position.Done(result as DecisionOutcome, trail.toList())
    }
}

/**
 * Procedura guidata che percorre l'albero decisionale di un item: una domanda alla volta, con la
 * possibilità di tornare indietro; alla fine mostra il livello raggiunto e lo applica con «Conferma».
 */
@Composable
fun DecisionTreeDialog(
    item: ToolItem,
    onConfirm: (path: List<Int>) -> Unit,
    onDismiss: () -> Unit,
) {
    val tree = item.decisionTree ?: return
    // Il percorso è salvato come "0,1,0" per sopravvivere alla rotazione dello schermo.
    var pathText by rememberSaveable(item.id) { mutableStateOf("") }
    val path = pathText.split(',').filter { it.isNotEmpty() }.map { it.toInt() }
    val position = tree.position(path)

    fun answer(index: Int) {
        pathText = (path + index).joinToString(",")
    }

    fun back() {
        pathText = path.dropLast(1).joinToString(",")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Albero decisionale", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Text(item.title, style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                when (position) {
                    is Position.Pending -> PendingContent(position, ::answer)
                    is Position.Done -> DoneContent(item, position)
                }
            }
        },
        confirmButton = {
            val done = position as? Position.Done
            Button(
                onClick = { if (done != null) onConfirm(path) },
                enabled = done != null,
            ) { Text(if (done != null) "Usa il livello ${done.outcome.score}" else "Rispondi alle domande") }
        },
        dismissButton = {
            Row {
                if (path.isNotEmpty()) {
                    TextButton(onClick = ::back) { Text("Indietro") }
                }
                TextButton(onClick = onDismiss) { Text("Annulla") }
            }
        },
    )
}

@Composable
private fun PendingContent(position: Position.Pending, onAnswer: (Int) -> Unit) {
    val question = position.question
    if (position.partTitle != null) {
        Text(
            position.partTitle.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(6.dp))
    }
    if (position.partScores.isNotEmpty()) {
        position.partScores.forEach { (title, score) ->
            Text(
                "$title: livello $score",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
    }
    if (position.trail.isNotEmpty()) {
        Text(
            "Risposte date: ${position.trail.joinToString(" → ")}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            question.text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(14.dp),
        )
    }
    Spacer(Modifier.height(14.dp))
    if (question.answers.size == 2 && question.answers.all { it.label.length <= 3 }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            question.answers.forEachIndexed { index, answer ->
                if (index == 0) {
                    Button(onClick = { onAnswer(index) }, modifier = Modifier.weight(1f)) { Text(answer.label) }
                } else {
                    OutlinedButton(onClick = { onAnswer(index) }, modifier = Modifier.weight(1f)) { Text(answer.label) }
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            question.answers.forEachIndexed { index, answer ->
                OutlinedButton(onClick = { onAnswer(index) }, modifier = Modifier.fillMaxWidth()) { Text(answer.label) }
            }
        }
    }
}

@Composable
private fun DoneContent(item: ToolItem, position: Position.Done) {
    val outcome = position.outcome
    val option = item.options.firstOrNull { it.score == outcome.score }
    if (position.trail.isNotEmpty()) {
        Text(
            "Risposte date: ${position.trail.joinToString(" → ")}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
    }
    if (outcome.partScores.size > 1) {
        outcome.partScores.forEach { (title, score) ->
            Text("$title: livello $score", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Il livello della voce è il più basso tra i due.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${outcome.score}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Livello ${outcome.score}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Text(outcome.label ?: option?.title ?: "", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    if (option != null && option.description.isNotBlank()) {
        Spacer(Modifier.height(10.dp))
        Text(
            option.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
