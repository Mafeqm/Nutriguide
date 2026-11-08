package com.nutriguide.config;

import com.nutriguide.modules.documento.DocumentoRepository;
import com.nutriguide.modules.documento.DocumentoResponse;
import com.nutriguide.modules.documento.DocumentoService;
import com.nutriguide.modules.documento.DocumentoTextoRequest;
import com.nutriguide.modules.documento.EstadoProcesamiento;
import com.nutriguide.ia.N8nClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Al arrancar:
 * <ol>
 *   <li>Registra los documentos educativos de src/main/resources/initial-docs que aún no existan.</li>
 *   <li>Si el servicio de IA (n8n) está configurado, indexa en Qdrant los documentos pendientes o con error.</li>
 * </ol>
 * Los alimentos y las recetas iniciales se cargan con SQL (db/data.sql), no aquí.
 */
@Configuration
public class DocumentSeeder {

    private static final Logger log = LoggerFactory.getLogger(DocumentSeeder.class);

    private static final String PROCEDENCIA = "Síntesis educativa del equipo NutriGuide. Referencias: ";

    private record Semilla(String archivo, String titulo, String descripcion, String categoria, String referencias) {
    }

    private static final List<Semilla> SEMILLAS = List.of(
            new Semilla("initial-docs/guia_cereales_avena_fao.txt",
                    "Guía educativa sobre cereales integrales, avena, quinua y fibra dietética",
                    "Composición de los granos enteros, nutrientes de la avena por 100 g, función del betaglucano y propiedades de la quinua.",
                    "Cereales y carbohidratos complejos",
                    "FAO y USDA FoodData Central"),
            new Semilla("initial-docs/guia_proteinas_legumbres_oms.txt",
                    "Guía educativa sobre proteínas, leguminosas y biodisponibilidad del hierro",
                    "Aminoácidos esenciales, proteínas animales y vegetales, complementación entre cereales y legumbres y absorción del hierro no hemo.",
                    "Proteínas y leguminosas",
                    "OMS, FAO e ICBF (Guías Alimentarias GABA)"),
            new Semilla("initial-docs/guia_frutas_verduras_micronutrientes_icbf.txt",
                    "Guía educativa sobre frutas, verduras, vitaminas y minerales",
                    "Consumo diario de frutas y verduras, fruta entera frente a jugos, vitamina C, potasio y cocción saludable de verduras.",
                    "Vitaminas, minerales, frutas y verduras",
                    "ICBF (Guías Alimentarias GABA) y OMS"),
            new Semilla("initial-docs/guia_grasas_saludables_hidratacion_oms.txt",
                    "Guía educativa sobre grasas saludables, omega-3, azúcares e hidratación",
                    "Grasas monoinsaturadas, omega-3 EPA/DHA, límites de azúcares libres y sodio de la OMS e hidratación saludable.",
                    "Grasas, hidratación y hábitos",
                    "OMS y FAO"),
            new Semilla("initial-docs/guia_hierro_zinc_micronutrientes_fao.txt",
                    "Guía educativa sobre hierro, zinc, calcio y vitamina D",
                    "Funciones de los micronutrientes esenciales, salud ósea, calcio y vitamina D, y fuentes alimentarias de hierro y zinc.",
                    "Vitaminas, minerales, frutas y verduras",
                    "FAO y OMS"));

    @Value("${nutriguide.seed.documents-enabled:true}")
    private boolean seedEnabled;

    @Bean
    @Order(2)
    public ApplicationRunner indexarDocumentosIniciales(DocumentoRepository documentoRepository,
                                                        DocumentoService documentoService,
                                                        N8nClient n8nClient) {
        return args -> {
            if (seedEnabled) {
                for (Semilla semilla : SEMILLAS) {
                    if (documentoRepository.findByTituloIgnoreCase(semilla.titulo()).isPresent()) {
                        continue;
                    }
                    try (InputStream in = new ClassPathResource(semilla.archivo()).getInputStream()) {
                        DocumentoTextoRequest req = new DocumentoTextoRequest();
                        req.setTitulo(semilla.titulo());
                        req.setDescripcion(semilla.descripcion());
                        req.setCategoria(semilla.categoria());
                        req.setFuenteOrigen(PROCEDENCIA + semilla.referencias());
                        req.setContenido(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                        DocumentoResponse doc = documentoService.cargarTexto(req);
                        log.info("Documento inicial '{}' registrado: {}", doc.titulo(), doc.estadoProcesamiento());
                    } catch (Exception ex) {
                        log.error("No se pudo registrar el documento inicial '{}': {}", semilla.titulo(), ex.getMessage());
                    }
                }
            }

            if (n8nClient.estaConfigurado()) {
                List<DocumentoResponse> procesados = documentoService.reprocesarPendientes();
                long correctos = procesados.stream()
                        .filter(d -> d.estadoProcesamiento() == EstadoProcesamiento.PROCESADO).count();
                if (!procesados.isEmpty()) {
                    log.info("Indexación al arrancar: {} de {} documentos procesados.", correctos, procesados.size());
                }
            }
        };
    }
}
