package com.kanayama.sudokuassistant.model

data class TwentyFourProgress(
    val round: TwentyFourRound,
    val focus: Int,
    val source: Int?,
    val operation: ArithmeticOperation?,
    val message: String,
) {
    fun encode(): String = listOf(
        "1", round.initialNumbers.joinToString(","),
        round.history.joinToString(";") { "${it.source},${it.target},${it.operation.name}" },
        focus.toString(), source?.toString().orEmpty(), operation?.name.orEmpty(), message,
    ).joinToString("|")

    companion object {
        fun decode(encoded: String): TwentyFourProgress? = runCatching {
            val fields = encoded.split('|', limit = 7)
            require(fields.size == 7 && fields[0] == "1")
            val numbers = fields[1].split(',').map { it.toInt() }
            require(TwentyFourGenerator.findSolution(numbers) != null)
            val round = TwentyFourRound(numbers)
            val moves = fields[2].split(';').filter { it.isNotEmpty() }
            require(moves.size <= 3)
            moves.forEach { move ->
                val parts = move.split(',')
                require(parts.size == 3 && round.remainingCount > 1)
                val result = round.combine(parts[0].toInt(), parts[1].toInt(), ArithmeticOperation.valueOf(parts[2]))
                require(result.status in listOf(TwentyFourMoveStatus.APPLIED, TwentyFourMoveStatus.NOT_TWENTY_FOUR))
            }
            val focus = fields[3].toInt()
            val source = fields[4].takeIf { it.isNotEmpty() }?.toInt()
            val operation = fields[5].takeIf { it.isNotEmpty() }?.let(ArithmeticOperation::valueOf)
            require(focus in 0..10 && (focus >= 4 || round.values[focus] != null))
            require(source == null || (source in 0..3 && round.values[source] != null))
            require(operation == null || source != null)
            TwentyFourProgress(round, focus, source, operation, fields[6])
        }.getOrNull()
    }
}
