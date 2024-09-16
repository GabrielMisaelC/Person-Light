package br.com.misael.software.personlight.Utils

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import br.com.misael.software.personlight.HouseData

class HouseViewModel : ViewModel() {
    // Lista de casas como um estado mutável
    val houses = mutableStateListOf<HouseData>()
}