package br.com.senai.sublime_app.user.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.senai.sublime_app.user.domain.User;
import br.com.senai.sublime_app.user.dto.UserRequestDTO;
import br.com.senai.sublime_app.user.dto.UserResponseDTO;
import br.com.senai.sublime_app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponseDTO create(UserRequestDTO dto) {
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setRole(dto.getRole());

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public List<UserResponseDTO> findAll() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponseDTO findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toResponse(user);
    }

    public UserResponseDTO update(Long id, UserRequestDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setRole(dto.getRole());

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }

    private UserResponseDTO toResponse(User user) {
        return new UserResponseDTO(user.getId(), user.getEmail(), user.getRole());
    }
}
