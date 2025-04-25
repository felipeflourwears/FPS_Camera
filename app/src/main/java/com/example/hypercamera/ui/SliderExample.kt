import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun SliderExample(
    minValue: Float,
    maxValue: Float,
    initialValue: Float,
    onValueChange: (Float) -> Unit
) {
    var sliderValue by remember { mutableStateOf(initialValue) }

    // Calcular el valor de shutter speed (1/valor)
    val shutterSpeed = if (sliderValue != 0f) {
        "1/${(1 / sliderValue).toInt()}"
    } else {
        "1/∞"
    }


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 120.dp)  // Reducido el padding para un mejor ajuste
    ) {
        Text(
            text = "Shutter speed: $shutterSpeed",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
            color = Color.White
        )
        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onValueChange(it)
            },
            valueRange = minValue..maxValue,  // El rango de 1/62 a 1/40
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                thumbColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .padding(horizontal = 32.dp)
        )
    }
}
