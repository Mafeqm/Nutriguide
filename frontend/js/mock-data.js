/**
 * DATOS DE PRUEBA — solo se usan si NUTRIGUIDE_CONFIG.USE_MOCK_DATA === true.
 * No representan información nutricional oficial. El chat de IA no usa este archivo.
 */
window.MockData = {
  alimentos: [
    { id: 1, nombre: 'Avena', categoria: 'Cereales y Tubérculos', descripcion: 'Cereal integral rico en fibra soluble (beta-glucanos).', calorias: 389, proteinas: 16.9, carbohidratos: 66.3, grasas: 6.9, fibra: 10.6, tamanoPorcion: 100, unidadPorcion: 'g', fuente: 'Dato de prueba' },
    { id: 2, nombre: 'Manzana', categoria: 'Frutas', descripcion: 'Fruta con fibra y vitamina C.', calorias: 52, proteinas: 0.3, carbohidratos: 13.8, grasas: 0.2, fibra: 2.4, tamanoPorcion: 100, unidadPorcion: 'g', fuente: 'Dato de prueba' },
    { id: 3, nombre: 'Lentejas', categoria: 'Leguminosas', descripcion: 'Legumbre con alto contenido de proteína vegetal y hierro.', calorias: 116, proteinas: 9, carbohidratos: 20, grasas: 0.4, fibra: 7.9, tamanoPorcion: 100, unidadPorcion: 'g', fuente: 'Dato de prueba' },
    { id: 4, nombre: 'Pechuga de pollo', categoria: 'Proteínas Animales', descripcion: 'Fuente de proteína magra.', calorias: 165, proteinas: 31, carbohidratos: 0, grasas: 3.6, fibra: 0, tamanoPorcion: 100, unidadPorcion: 'g', fuente: 'Dato de prueba' },
    { id: 5, nombre: 'Brócoli', categoria: 'Verduras y Hortalizas', descripcion: 'Verdura crucífera con vitamina C y K.', calorias: 34, proteinas: 2.8, carbohidratos: 6.6, grasas: 0.4, fibra: 2.6, tamanoPorcion: 100, unidadPorcion: 'g', fuente: 'Dato de prueba' },
    { id: 6, nombre: 'Arroz integral', categoria: 'Cereales y Tubérculos', descripcion: 'Grano entero con más fibra que el arroz blanco.', calorias: 111, proteinas: 2.6, carbohidratos: 23, grasas: 0.9, fibra: 1.8, tamanoPorcion: 100, unidadPorcion: 'g', fuente: 'Dato de prueba' },
  ],
  recetas: [
    { id: 1, nombre: 'Avena con manzana', categoria: 'Desayunos', tiempoPreparacion: '10 min', descripcion: 'Desayuno sencillo y saciante.', ingredientes: ['1/2 taza de avena', '1 taza de agua o leche', '1 manzana picada', 'Canela al gusto'], pasos: ['Cocina la avena con el líquido a fuego medio 5 minutos.', 'Agrega la manzana picada y la canela.', 'Sirve caliente.'], calorias: 280, proteinas: 9, carbohidratos: 48, grasas: 5, fibra: 7 },
    { id: 2, nombre: 'Arroz con verduras', categoria: 'Almuerzos', tiempoPreparacion: '30 min', descripcion: 'Plato completo y colorido.', ingredientes: ['1 taza de arroz integral', '1 taza de brócoli', '1 zanahoria', '1 cda de aceite de oliva'], pasos: ['Cocina el arroz según el paquete.', 'Saltea las verduras con el aceite.', 'Mezcla y sirve.'], calorias: 340, proteinas: 8, carbohidratos: 60, grasas: 7, fibra: 6 },
    { id: 3, nombre: 'Guiso de lentejas', categoria: 'Almuerzos', tiempoPreparacion: '45 min', descripcion: 'Rico en proteína vegetal.', ingredientes: ['1 taza de lentejas', '1 cebolla', '2 dientes de ajo', '1 tomate'], pasos: ['Remoja las lentejas.', 'Sofríe cebolla, ajo y tomate.', 'Añade las lentejas y agua; cocina 30 minutos.'], calorias: 310, proteinas: 18, carbohidratos: 48, grasas: 4, fibra: 14 },
  ],
  documentos: [
    { id: 1, titulo: 'Alimentación equilibrada', categoria: 'Hábitos', descripcion: 'Principios básicos para construir un plato balanceado.', contenido: 'Contenido de prueba: un plato equilibrado combina verduras, cereales integrales, proteínas y grasas saludables.' },
    { id: 2, titulo: 'Macronutrientes', categoria: 'Nutrientes', descripcion: 'Carbohidratos, proteínas y grasas: funciones y fuentes.', contenido: 'Contenido de prueba sobre macronutrientes.' },
    { id: 3, titulo: 'Vitaminas y minerales', categoria: 'Nutrientes', descripcion: 'Micronutrientes esenciales y dónde encontrarlos.', contenido: 'Contenido de prueba sobre micronutrientes.' },
    { id: 4, titulo: 'Hidratación', categoria: 'Hábitos', descripcion: 'Por qué el agua es clave para la salud.', contenido: 'Contenido de prueba sobre hidratación.' },
  ],
};
