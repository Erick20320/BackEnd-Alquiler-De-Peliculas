package net.java.springboot.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.java.springboot.model.Pelicula;
import net.java.springboot.repository.PeliculaRepository;

@CrossOrigin(origins = "http://192.168.1.24:3000")
@RestController
@RequestMapping("/api/v1/")
public class PeliculaController {

	@Autowired
	private PeliculaRepository peliculaRepository;

	// Obtener todas las peliculas

	@GetMapping("/peliculas")
	public List<Pelicula> getAllPeliculas() {
		return peliculaRepository.findAll();
	}

	// Crear Peliculas

	@PostMapping("/peliculas")
	public ResponseEntity<Pelicula> createPelicula(@RequestParam("imagen") MultipartFile imagen,
			@RequestParam("pelicula") String peliculaJson) throws IOException {

		try {
			if (imagen.isEmpty()) {
				// La imagen es obligatoria
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			// Obtener los bytes de la imagen
			byte[] imagenBytes = imagen.getBytes();

			// Crear una instancia de la película con los datos proporcionados
			Pelicula pelicula = new ObjectMapper().readValue(peliculaJson, Pelicula.class);
			Pelicula nuevaPelicula = peliculaRepository.save(pelicula);

			// Obtener el ID de la película
			Long peliculaId = nuevaPelicula.getId();

			// Generar un nombre único para el archivo de imagen
			String nombreImagen = peliculaId + "_" + UUID.randomUUID().toString() + "_" + imagen.getOriginalFilename();

			// Ruta del directorio de imágenes dentro del proyecto
			String directorioImagenes = "src/main/resources/static/img/";

			// Obtener la ruta absoluta del directorio de imágenes
			String rutaDirectorioImagenes = new File(directorioImagenes).getAbsolutePath();

			// Guardar la imagen en el directorio
			String rutaImagen = rutaDirectorioImagenes + File.separator + nombreImagen;
			Path rutaImagenPath = Paths.get(rutaImagen);
			Files.write(rutaImagenPath, imagenBytes);

			// Obtener la URL completa de la imagen
			String urlBaseBackend = "http://192.168.1.24:8080/api/v1/peliculas/"; // Cambia esto con la URL base de tu
																					// backend
			String urlImagenCompleta = urlBaseBackend + "img/" + nombreImagen;

			// Actualizar la ruta de la imagen en la película
			nuevaPelicula.setImagen(urlImagenCompleta);
			peliculaRepository.save(nuevaPelicula);

			return new ResponseEntity<>(nuevaPelicula, HttpStatus.CREATED);
		} catch (Exception e) {
			// Manejar el error y devolver una respuesta de error adecuada
			System.err.println("Error al guardar la imagen: " + e.getMessage());
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// Obtener peliculas por id

	@GetMapping("/peliculas/{id}")
	public ResponseEntity<Pelicula> getPeliculaPorId(@PathVariable Long id) {
		Pelicula pelicula = peliculaRepository.findById(id).orElse(null);

		if (pelicula != null) {
			return new ResponseEntity<>(pelicula, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}

	// Actualizar película
	@PutMapping("/peliculas/{id}")
	public ResponseEntity<Pelicula> updatePelicula(@PathVariable Long id,
			@RequestParam(value = "imagen", required = false) MultipartFile imagen,
			@RequestParam(value = "pelicula", required = false) String peliculaJson) throws IOException {

		try {
			Pelicula pelicula = peliculaRepository.findById(id).orElse(null);

			if (pelicula != null) {
				if (peliculaJson != null) {
					ObjectMapper objectMapper = new ObjectMapper();
					Pelicula peliculaDatos = objectMapper.readValue(peliculaJson, Pelicula.class);
					pelicula.setTitulo(peliculaDatos.getTitulo());
					pelicula.setGenero(peliculaDatos.getGenero());
					pelicula.setAnioEstreno(peliculaDatos.getAnioEstreno());
					pelicula.setDuracionMinutos(peliculaDatos.getDuracionMinutos());
					pelicula.setDirector(peliculaDatos.getDirector());
					pelicula.setSinopsis(peliculaDatos.getSinopsis());
					pelicula.setDisponible(peliculaDatos.getDisponible());
				}

				if (imagen != null && !imagen.isEmpty()) {
					String nombreImagen = pelicula.getId() + "_" + UUID.randomUUID().toString() + "_"
							+ imagen.getOriginalFilename();
					String rutaImagen = "src/main/resources/static/img/" + nombreImagen;
					Path rutaImagenPath = Paths.get(rutaImagen);
					Files.write(rutaImagenPath, imagen.getBytes());

					// Agregar la URL base del backend al nombre de la imagen
					String urlBaseBackend = "http://192.168.1.24:8080/api/v1/peliculas/";
					String urlImagenCompleta = urlBaseBackend + "img/" + nombreImagen;

					pelicula.setImagen(urlImagenCompleta);
				}

				Pelicula peliculaActualizada = peliculaRepository.save(pelicula);
				return new ResponseEntity<>(peliculaActualizada, HttpStatus.OK);
			} else {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// Eliminar pelicula por id

	@DeleteMapping("/peliculas/{id}")
	public ResponseEntity<?> deletePelicula(@PathVariable Long id) {
		try {
			peliculaRepository.deleteById(id);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}