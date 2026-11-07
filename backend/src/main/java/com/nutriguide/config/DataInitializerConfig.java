package com.nutriguide.config;

import com.nutriguide.modules.alimento.Alimento;
import com.nutriguide.modules.alimento.AlimentoRepository;
import com.nutriguide.modules.documento.DocumentoRepository;
import com.nutriguide.modules.documento.DocumentoService;
import com.nutriguide.modules.documento.DocumentoTextoRequest;
import com.nutriguide.modules.receta.Receta;
import com.nutriguide.modules.receta.RecetaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Inicializador automático de datos semilla (Alimentos, Recetas y Documentos Educativos con Embeddings en pgvector).
 * Garantiza que la plataforma tenga datos verificables listos para probar la API REST y la recuperación semántica.
 */
@Configuration
public class DataInitializerConfig {

    private static final Logger log = LoggerFactory.getLogger(DataInitializerConfig.class);

    @Value("${nutriguide.seed.enabled:true}")
    private boolean seedEnabled;

    @Bean
    @Order(2)
    public CommandLineRunner seedInitialData(
            AlimentoRepository alimentoRepository,
            RecetaRepository recetaRepository,
            DocumentoRepository documentoRepository,
            DocumentoService documentoService) {
        return args -> {
            if (!seedEnabled) {
                log.info("Carga de datos semilla deshabilitada por configuración (nutriguide.seed.enabled=false).");
                return;
            }

            inicializarAlimentos(alimentoRepository);
            inicializarRecetas(recetaRepository);
            inicializarDocumentosEducativos(documentoRepository, documentoService);
        };
    }

    private void inicializarAlimentos(AlimentoRepository alimentoRepository) {
        if (alimentoRepository.count() > 0) {
            log.info("La tabla 'alimentos' ya cuenta con {} registros.", alimentoRepository.count());
            return;
        }

        List<Alimento> alimentos = List.of(
                new Alimento(
                        "Avena en hojuelas integral",
                        "Cereal de grano entero rico en fibra soluble tipo betaglucano, carbohidratos complejos de absorción lenta, proteínas vegetales y minerales como magnesio, fósforo, hierro y zinc.",
                        "Cereales y Tubérculos",
                        "https://images.unsplash.com/photo-1517673132405-a56a62b18caf?auto=format&fit=crop&w=800&q=80",
                        new BigDecimal("389.00"), new BigDecimal("16.89"), new BigDecimal("66.27"),
                        new BigDecimal("6.90"), new BigDecimal("10.60"), new BigDecimal("100.00"), "g",
                        "USDA FoodData Central (FDC ID: 173904) - Grano entero crudo por 100 g"
                ),
                new Alimento(
                        "Quinua en grano cruda",
                        "Pseudocereal andino de alto valor biológico que aporta los nueve aminoácidos esenciales, fibra dietética, hierro no hemo, magnesio y compuestos antioxidantes.",
                        "Cereales y Tubérculos",
                        "https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=800&q=80",
                        new BigDecimal("368.00"), new BigDecimal("14.12"), new BigDecimal("64.16"),
                        new BigDecimal("6.07"), new BigDecimal("7.00"), new BigDecimal("100.00"), "g",
                        "USDA FoodData Central (FDC ID: 168874) / FAO Año Internacional de la Quinua - Por 100 g"
                ),
                new Alimento(
                        "Lenteja seca cruda",
                        "Leguminosa rica en proteína vegetal, fibra dietética soluble e insoluble, folatos (vitamina B9), hierro y potasio, con bajo contenido de grasa.",
                        "Leguminosas",
                        "https://images.unsplash.com/photo-1515543904379-3d757afe72e4?auto=format&fit=crop&w=800&q=80",
                        new BigDecimal("352.00"), new BigDecimal("24.63"), new BigDecimal("63.35"),
                        new BigDecimal("1.06"), new BigDecimal("10.70"), new BigDecimal("100.00"), "g",
                        "USDA FoodData Central (FDC ID: 172420) / Tabla de Composición de Alimentos ICBF - Por 100 g"
                ),
                new Alimento(
                        "Garbanzo seco crudo",
                        "Leguminosa fuente de carbohidratos complejos, proteínas vegetales, fibra dietética, folatos, manganeso y hierro. Combinado con cereales ofrece proteína completa.",
                        "Leguminosas",
                        "https://images.unsplash.com/photo-1515543237350-b3eea1ec8082?auto=format&fit=crop&w=800&q=80",
                        new BigDecimal("378.00"), new BigDecimal("20.47"), new BigDecimal("62.95"),
                        new BigDecimal("6.04"), new BigDecimal("12.20"), new BigDecimal("100.00"), "g",
                        "USDA FoodData Central (FDC ID: 173756) - Por 100 g"
                ),
                new Alimento(
                        "Pechuga de pollo sin piel cocida",
                        "Fuente de proteína animal magra de alto valor biológico, rica en vitaminas del complejo B (niacina B3 y piridoxina B6), fósforo y selenio.",
                        "Proteínas Animales",
                        "https://images.unsplash.com/photo-1604503468506-a8da13d82791?auto=format&fit=crop&w=800&q=80",
                        new BigDecimal("165.00"), new BigDecimal("31.02"), new BigDecimal("0.00"),
                        new BigDecimal("3.57"), new BigDecimal("0.00"), new BigDecimal("100.00"), "g",
                        "USDA FoodData Central (FDC ID: 171477) - Pechuga asada sin piel por 100 g"
                ),
                new Alimento(
                        "Salmón del Atlántico cocido",
                        "Pescado azul rico en proteínas de alta calidad, ácidos grasos poliinsaturados omega-3 (EPA y DHA), vitamina D y vitamina B12.",
                        "Proteínas Animales",
                        "https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?auto=format&fit=crop&w=800&q=80",
                        new BigDecimal("206.00"), new BigDecimal("22.10"), new BigDecimal("0.00"),
                        new BigDecimal("12.35"), new BigDecimal("0.00"), new BigDecimal("100.00"), "g",
                        "USDA FoodData Central (FDC ID: 175168) - Cocción al calor seco por 100 g"
                ),
                new Alimento(
                        "Huevo de gallina entero cocido",
                        "Alimento con proteína de referencia de altísima biodisponibilidad, colina, luteína, zeaxantina, vitaminas A, D, B12 y selenio.",
                        "Proteínas Animales",
                        "https://images.unsplash.com/photo-1506976785307-8732e854ad03?auto=format&fit=crop&w=800&q=80",
                        new BigDecimal("155.00"), new BigDecimal("12.58"), new BigDecimal("1.12"),
                        new BigDecimal("10.61"), new BigDecimal("0.00"), new BigDecimal("100.00"), "g",
                        "USDA FoodData Central (FDC ID: 173424) / ICBF TCAC - Parte comestible por 100 g"
                ),
                new Alimento(
                        "Espinaca fresca cruda",
                        "Hortaliza de hoja verde oscura con baja densidad calórica y alto aporte de vitamina K, provitamina A (betacaroteno), folatos, vitamina C, hierro no hemo y magnesio.",
                        "Verduras y Hortalizas",
                        "https://images.unsplash.com/photo-1576045057995-568f588f82fb?auto=format&fit=crop&w=800&q=80",
                        new BigDecimal("23.00"), new BigDecimal("2.86"), new BigDecimal("3.63"),
                        new BigDecimal("0.39"), new BigDecimal("2.20"), new BigDecimal("100.00"), "g",
                        "USDA FoodData Central (FDC ID: 168462) / ICBF TCAC - Por 100 g"
                ),
                new Alimento(
                        "Brócoli fresco crudo",
                        "Vegetal crucífero rico en vitamina C, vitamina K, folatos, fibra dietética y compuestos fitoquímicos azufrados como el sulforafano.",
                        "Verduras y Hortalizas",
                        "https://images.unsplash.com/photo-1459411621453-7b03977f4bfc?auto=format&fit=crop&w=800&q=80",
                        new BigDecimal("34.00"), new BigDecimal("2.82"), new BigDecimal("6.64"),
                        new BigDecimal("0.37"), new BigDecimal("2.60"), new BigDecimal("100.00"), "g",
                        "USDA FoodData Central (FDC ID: 170379) - Por 100 g"
                ),
                new Alimento(
                        "Aguacate Hass fresco",
                        "Fruta oleaginosa rica en ácidos grasos monoinsaturados (ácido oleico), fibra dietética, potasio, vitamina E, vitamina K y folatos.",
                        "Grasas Saludables y Semillas",
                        "https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?auto=format&fit=crop&w=800&q=80",
                        new BigDecimal("160.00"), new BigDecimal("2.00"), new BigDecimal("8.53"),
                        new BigDecimal("14.66"), new BigDecimal("6.70"), new BigDecimal("100.00"), "g",
                        "USDA FoodData Central (FDC ID: 171705) / ICBF TCAC - Pulpa fresca por 100 g"
                ),
                new Alimento(
                        "Almendras naturales sin sal",
                        "Fruto seco rico en grasas monoinsaturadas, proteína vegetal, fibra dietética, vitamina E (alfa-tocoferol), magnesio y calcio.",
                        "Grasas Saludables y Semillas",
                        "https://images.unsplash.com/photo-1508061253366-f7da158b6d46?auto=format&fit=crop&w=800&q=80",
                        new BigDecimal("579.00"), new BigDecimal("21.15"), new BigDecimal("21.55"),
                        new BigDecimal("49.93"), new BigDecimal("12.50"), new BigDecimal("100.00"), "g",
                        "USDA FoodData Central (FDC ID: 170567) - Por 100 g"
                ),
                new Alimento(
                        "Guayaba común fresca",
                        "Fruta tropical de altísimo contenido en vitamina C (ácido ascórbico), fibra dietética soluble (pectina), potasio y licopeno.",
                        "Frutas",
                        "https://images.unsplash.com/photo-1536511132770-e5058c7e8c46?auto=format&fit=crop&w=800&q=80",
                        new BigDecimal("68.00"), new BigDecimal("2.55"), new BigDecimal("14.32"),
                        new BigDecimal("0.95"), new BigDecimal("5.40"), new BigDecimal("100.00"), "g",
                        "Tabla de Composición de Alimentos Colombianos ICBF / USDA FDC ID: 173044 - Por 100 g"
                ),
                new Alimento(
                        "Banano maduro fresco",
                        "Fruta energética fuente de carbohidratos de fácil digestión, potasio, vitamina B6 (piridoxina), vitamina C y fibra soluble.",
                        "Frutas",
                        "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?auto=format&fit=crop&w=800&q=80",
                        new BigDecimal("89.00"), new BigDecimal("1.09"), new BigDecimal("22.84"),
                        new BigDecimal("0.33"), new BigDecimal("2.60"), new BigDecimal("100.00"), "g",
                        "USDA FoodData Central (FDC ID: 173944) / ICBF TCAC - Pulpa por 100 g"
                ),
                new Alimento(
                        "Yogur griego natural sin azúcar descremado",
                        "Derivado lácteo fermentado con alta concentración de proteínas de alto valor biológico, calcio biodisponible, fósforo y cultivos probióticos.",
                        "Lácteos y Derivados",
                        "https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=800&q=80",
                        new BigDecimal("59.00"), new BigDecimal("10.19"), new BigDecimal("3.60"),
                        new BigDecimal("0.39"), new BigDecimal("0.00"), new BigDecimal("100.00"), "g",
                        "USDA FoodData Central (FDC ID: 170903) - Por 100 g"
                )
        );

        alimentoRepository.saveAll(alimentos);
        log.info("Cargados {} alimentos iniciales con porción de referencia estandarizada (100 g).", alimentos.size());
    }

    private void inicializarRecetas(RecetaRepository recetaRepository) {
        if (recetaRepository.count() > 0) {
            log.info("La tabla 'recetas' ya cuenta con {} registros.", recetaRepository.count());
            return;
        }

        List<Receta> recetas = List.of(
                new Receta(
                        "Porridge de Avena Integral con Banano y Almendras",
                        "Desayuno nutritivo basado en grano entero que combina fibra soluble (betaglucanos de la avena), potasio del banano y grasas monoinsaturadas de las almendras.",
                        "https://images.unsplash.com/photo-1517673132405-a56a62b18caf?auto=format&fit=crop&w=800&q=80",
                        "50 g de avena en hojuelas integral; 200 ml de leche semidescremada o bebida vegetal fortificada sin azúcar; 80 g de banano maduro en rodajas; 15 g de almendras naturales picadas; 2 g de canela en polvo.",
                        "1. En una olla pequeña, calentar los 200 ml de leche o bebida vegetal a fuego medio.\n2. Incorporar los 50 g de avena en hojuelas y la canela en polvo, removiendo constantemente durante 6 a 8 minutos hasta obtener una textura cremosa.\n3. Servir en un tazón y decorar con los 80 g de banano en rodajas y los 15 g de almendras picadas.",
                        12,
                        "Por porción servida (1 tazón de 345 g): 415 kcal | Proteínas: 16.5 g | Carbohidratos: 58.2 g | Grasas totales: 12.4 g | Fibra dietética: 8.6 g (Cálculo basado en USDA FoodData Central).",
                        "Desayunos Saludables"
                ),
                new Receta(
                        "Bowl Andino de Quinua, Garbanzos, Espinaca y Aguacate",
                        "Plato principal completo de origen vegetal que combina un pseudocereal (quinua) y una leguminosa (garbanzos) con verduras ricas en vitamina C para optimizar la absorción del hierro no hemo.",
                        "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=800&q=80",
                        "60 g de quinua en grano cruda (aprox. 180 g cocida); 80 g de garbanzos cocidos; 50 g de espinaca fresca lavada; 60 g de aguacate Hass en cubos; 50 g de tomate chonto picado; 10 ml de jugo de limón fresco; 5 ml de aceite de oliva virgen extra; pizca de sal yodada.",
                        "1. Lavar muy bien los 60 g de quinua bajo el chorro de agua fría frotando los granos para retirar las saponinas.\n2. Cocinar la quinua en el doble de agua durante 15 minutos hasta que el grano abra; dejar reposar.\n3. En un bol amplio, disponer una base con los 50 g de espinaca fresca y el tomate picado.\n4. Agregar la quinua cocida, los 80 g de garbanzos cocidos y los 60 g de aguacate en cubos.\n5. Aderezar con los 10 ml de jugo de limón (rico en vitamina C que favorece la absorción de hierro vegetal) y los 5 ml de aceite de oliva.",
                        25,
                        "Por porción servida (1 plato de 435 g): 520 kcal | Proteínas: 18.4 g | Carbohidratos: 66.8 g | Grasas totales: 19.2 g | Fibra dietética: 14.5 g (Cálculo basado en USDA FoodData Central / ICBF).",
                        "Almuerzos Equilibrados"
                ),
                new Receta(
                        "Guiso Educativo de Lentejas con Verduras y Brócoli al Vapor",
                        "Preparación tradicional rica en fibra dietética, folatos y proteína vegetal, acompañada de brócoli al vapor corto para preservar la vitamina C y los glucosinolatos.",
                        "https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=800&q=80",
                        "70 g de lentejas secas crudas; 100 g de brócoli fresco en ramilletes; 50 g de zanahoria picada; 40 g de cebolla cabezona; 30 g de pimentón rojo picado; 1 diente de ajo; 5 ml de aceite vegetal; comino y laurel al gusto.",
                        "1. Remojar las lentejas previamente durante 2 horas y escurrir.\n2. Cocinar los 70 g de lentejas en agua con cebolla, ajo, zanahoria y laurel durante 25 minutos a fuego medio.\n3. Cocinar los 100 g de brócoli al vapor durante solo 4 a 5 minutos para conservar su textura crujiente, color verde intenso y contenido de vitamina C.\n4. Servir las lentejas junto con el brócoli al vapor y el pimentón rojo.",
                        35,
                        "Por porción servida (1 plato de 390 g): 365 kcal | Proteínas: 21.2 g | Carbohidratos: 55.4 g | Grasas totales: 6.1 g | Fibra dietética: 11.8 g (Cálculo basado en USDA / ICBF TCAC).",
                        "Almuerzos Equilibrados"
                ),
                new Receta(
                        "Salmón al Horno con Brócoli y Ensalada Verde de Aguacate",
                        "Cena rica en ácidos grasos poliinsaturados omega-3 (EPA y DHA), proteínas de alto valor biológico, vitamina D y antioxidantes naturales.",
                        "https://images.unsplash.com/photo-1467003909585-2f8a72700288?auto=format&fit=crop&w=800&q=80",
                        "120 g de filete de salmón; 100 g de brócoli en ramilletes; 50 g de espinaca fresca; 50 g de aguacate Hass; 10 ml de jugo de limón; hierbas aromáticas (eneldo o romero) y pimienta negra.",
                        "1. Precalentar el horno a 180 °C. Sazonar el filete de 120 g de salmón con jugo de limón, hierbas aromáticas y pimienta.\n2. Hornear el salmón durante 12 a 15 minutos evitando sobrecocinar para preservar los ácidos grasos omega-3.\n3. Cocinar los 100 g de brócoli al vapor durante 4 minutos.\n4. Acompañar con la ensalada de espinaca fresca y aguacate.",
                        20,
                        "Por porción servida (1 plato de 320 g): 375 kcal | Proteínas: 30.8 g | Carbohidratos: 11.9 g | Grasas totales: 22.5 g | Fibra dietética: 7.1 g (Cálculo basado en USDA FoodData Central).",
                        "Cenas Ligeras"
                ),
                new Receta(
                        "Copa Proteica de Yogur Griego Natural con Guayaba y Avena",
                        "Merienda funcional de alta densidad nutricional que aporta proteínas lácteas, calcio biodisponible y más del 100% del requerimiento diario de vitamina C gracias a la guayaba.",
                        "https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=800&q=80",
                        "150 g de yogur griego natural sin azúcar descremado; 100 g de guayaba fresca picada en cubos sin semillas duras; 25 g de avena en hojuelas integral; 10 g de almendras fileteadas.",
                        "1. Lavar y picar los 100 g de guayaba fresca.\n2. En un vaso o copa, colocar una capa de yogur griego natural, seguida de la avena en hojuelas y los cubos de guayaba.\n3. Finalizar con las almendras fileteadas en la superficie.",
                        7,
                        "Por porción servida (1 copa de 285 g): 310 kcal | Proteínas: 24.1 g | Carbohidratos: 38.4 g | Grasas totales: 7.3 g | Fibra dietética: 9.2 g (Cálculo basado en ICBF TCAC / USDA).",
                        "Snacks y Meriendas"
                )
        );

        recetaRepository.saveAll(recetas);
        log.info("Cargadas {} recetas educativas iniciales.", recetas.size());
    }

    private void inicializarDocumentosEducativos(
            DocumentoRepository documentoRepository,
            DocumentoService documentoService) {

        if (documentoRepository.count() > 0) {
            log.info("La biblioteca RAG ya cuenta con {} documentos registrados.", documentoRepository.count());
            return;
        }

        record DocSemilla(String archivoClasspath, String titulo, String descripcion, String categoria, String fuente) {
        }

        List<DocSemilla> semillas = List.of(
                new DocSemilla(
                        "initial-docs/guia_cereales_avena_fao.txt",
                        "Guía Educativa sobre Cereales Integrales, Avena, Quinua y Fibra Dietética",
                        "Documento técnico-educativo sobre la composición de los granos enteros, nutrientes de la avena por 100 g, función fisiológica del betaglucano y propiedades de la quinua.",
                        "Cereales y Carbohidratos Complejos",
                        "FAO y USDA FoodData Central"
                ),
                new DocSemilla(
                        "initial-docs/guia_proteinas_legumbres_oms.txt",
                        "Guía Educativa sobre Proteínas, Leguminosas y Biodisponibilidad del Hierro",
                        "Documento educativo sobre aminoácidos esenciales, proteínas animales y vegetales, complementación proteica entre cereales y legumbres, y absorción del hierro no hemo con vitamina C.",
                        "Proteínas y Leguminosas",
                        "OMS, FAO e ICBF (Guías Alimentarias GABA)"
                ),
                new DocSemilla(
                        "initial-docs/guia_frutas_verduras_micronutrientes_icbf.txt",
                        "Guía Educativa sobre Frutas, Verduras, Vitaminas, Minerales y Compuestos Bioactivos",
                        "Guía sobre el aporte diario de frutas y verduras, importancia de la fruta entera frente a jugos, vitamina C en la guayaba, potasio en el banano y cocción saludable de brócoli y espinaca.",
                        "Micronutrientes, Frutas y Verduras",
                        "ICBF (Guías Alimentarias GABA) y OMS"
                ),
                new DocSemilla(
                        "initial-docs/guia_grasas_saludables_hidratacion_oms.txt",
                        "Guía Educativa sobre Grasas Saludables, Ácidos Grasos Omega-3, Límite de Azúcares e Hidratación",
                        "Directrices educativas sobre ácidos grasos monoinsaturados (aguacate y almendras), omega-3 EPA/DHA (salmón), límites de azúcares libres y sodio de la OMS, e hidratación saludable.",
                        "Lípidos, Hidratación y Prevención Nutricional",
                        "Organización Mundial de la Salud (OMS) y FAO"
                )
        );

        for (DocSemilla semilla : semillas) {
            try {
                ClassPathResource resource = new ClassPathResource(semilla.archivoClasspath());
                try (InputStream is = resource.getInputStream()) {
                    String contenido = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    DocumentoTextoRequest req = new DocumentoTextoRequest();
                    req.setTitulo(semilla.titulo());
                    req.setDescripcion(semilla.descripcion());
                    req.setCategoria(semilla.categoria());
                    req.setFuenteOrigen(semilla.fuente());
                    req.setContenido(contenido);
                    documentoService.cargarDocumentoDesdeTexto(req);
                }
            } catch (Exception ex) {
                log.error("Error cargando documento educativo semilla '{}': {}",
                        semilla.titulo(), ex.getMessage());
            }
        }
    }
}

