package com.dookia.teamflow.workspace.repository;

import com.dookia.teamflow.user.entity.User;
import com.dookia.teamflow.user.repository.UserRepository;
import com.dookia.teamflow.workspace.entity.Workspace;
import com.dookia.teamflow.workspace.entity.WorkspaceInvitation;
import com.dookia.teamflow.workspace.entity.WorkspaceInvitationStatus;
import com.dookia.teamflow.workspace.entity.WorkspaceMember;
import com.dookia.teamflow.workspace.entity.WorkspaceMemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class WorkspaceRepositoryTest {

    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired WorkspaceInvitationRepository workspaceInvitationRepository;
    @Autowired UserRepository userRepository;
    @Autowired TestEntityManager em;

    @Test
    @DisplayName("Workspace.create 로 저장하면 slug 가 전역 유일하게 생성된다")
    void save_generatesUniqueSlug() {
        Workspace a = workspaceRepository.save(Workspace.create("Team Alpha"));
        Workspace b = workspaceRepository.save(Workspace.create("Team Alpha"));
        em.flush();
        em.clear();

        assertThat(a.getSlug()).isNotEqualTo(b.getSlug());
        assertThat(a.getSlug()).startsWith("team-alpha-");
        assertThat(workspaceRepository.existsBySlug(a.getSlug())).isTrue();
    }

    @Test
    @DisplayName("WorkspaceMember — userNo + workspaceNo 조합 고유 + 역할 enum 저장")
    void workspaceMember_roundTrip() {
        User u = userRepository.save(User.createFromGoogle("sub-1", "a@b.com", "A", null));
        Workspace ws = workspaceRepository.save(Workspace.create("W"));

        WorkspaceMember saved = workspaceMemberRepository.save(
            WorkspaceMember.of(ws.getNo(), u.getNo(), WorkspaceMemberRole.OWNER));
        em.flush();
        em.clear();

        assertThat(workspaceMemberRepository.findByWorkspaceNoAndUserNo(ws.getNo(), u.getNo()))
            .hasValueSatisfying(m -> {
                assertThat(m.getRole()).isEqualTo(WorkspaceMemberRole.OWNER);
                assertThat(m.getJoinDate()).isNotNull();
            });
        assertThat(workspaceMemberRepository.existsByWorkspaceNoAndUserNo(ws.getNo(), u.getNo())).isTrue();
        assertThat(workspaceMemberRepository.countByWorkspaceNo(ws.getNo())).isEqualTo(1);
        assertThat(saved.getNo()).isNotNull();
    }

    @Test
    @DisplayName("WorkspaceInvitation — 기본 TTL 7일 + PENDING 상태로 발급")
    void invitation_issuedWithPendingStatus() {
        User inviter = userRepository.save(User.createFromGoogle("s-i", "i@x.com", "I", null));
        User invitee = userRepository.save(User.createFromGoogle("s-v", "v@x.com", "V", null));
        Workspace ws = workspaceRepository.save(Workspace.create("W"));

        WorkspaceInvitation inv = workspaceInvitationRepository.save(
            WorkspaceInvitation.issue(ws.getNo(), invitee.getNo(), inviter.getNo(),
                WorkspaceMemberRole.MEMBER, "token-xyz"));
        em.flush();
        em.clear();

        assertThat(workspaceInvitationRepository.findByToken("token-xyz"))
            .hasValueSatisfying(found -> {
                assertThat(found.getStatus()).isEqualTo(WorkspaceInvitationStatus.PENDING);
                assertThat(found.getRole()).isEqualTo(WorkspaceMemberRole.MEMBER);
                assertThat(found.getExpireDate()).isAfter(found.getCreateDate());
            });
        assertThat(workspaceInvitationRepository
            .existsByWorkspaceNoAndInviteeUserNoAndStatus(ws.getNo(), invitee.getNo(), WorkspaceInvitationStatus.PENDING))
            .isTrue();
        assertThat(inv.getNo()).isNotNull();
    }
}
