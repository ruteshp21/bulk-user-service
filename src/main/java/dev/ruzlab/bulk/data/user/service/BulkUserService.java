package dev.ruzlab.bulk.data.user.service;

import dev.ruzlab.bulk.data.user.dao.UserDTO;
import dev.ruzlab.bulk.data.user.repository.UserRepository;
import dev.ruzlab.bulk.data.user.util.UserUtil;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class BulkUserService {

    private final Integer partitionSize;

    private final KafkaTemplate<String, List<UserDTO>> kafkaTemplate;

    private final UserRepository userRepository;

    public BulkUserService(@Value("${bulk.data.partitionSize}") Integer partitionSize,
                           KafkaTemplate<String, List<UserDTO>> kafkaTemplate,
                           UserRepository userRepository) {
        this.partitionSize = partitionSize;
        this.kafkaTemplate = kafkaTemplate;
        this.userRepository = userRepository;
    }

    public void processUsersInBulk(List<UserDTO> userDTOList) {
        int subArraySize = (userDTOList.size() <= partitionSize) ? userDTOList.size() : userDTOList.size() / partitionSize;
        System.out.println("Sub-Array Size: " + subArraySize);
        List<CompletableFuture<SendResult<String, List<UserDTO>>>> list = UserUtil.get(userDTOList, subArraySize).stream()
                .map(this::processOverPartition)
                .toList();
        list.forEach(val -> val.whenComplete((result, ex) -> {
            if(ex == null) {
                System.out.println("Success");
            } else {
                System.out.println("Error " + ex.getMessage());
            }
        }));


    }

    private CompletableFuture<SendResult<String, List<UserDTO>>> processOverPartition(List<UserDTO> userDTOList) {
        ProducerRecord<String,List<UserDTO>> producerRecord = new ProducerRecord<>("partition-users", userDTOList);
        return kafkaTemplate.send(producerRecord);
    }

    public void deleteAll() {
        userRepository.truncateTable();
    }


    /*public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> createParallelUsers(List<User> userList) {
        long start = System.currentTimeMillis();
        List<User> collect = userList.parallelStream()
                .map(this::userDtoToEntity)
                .map(userRepository::save)
                .map(this::userEntityToDto)
                .collect(Collectors.toList());
        // end of function
        long end = System.currentTimeMillis();
        System.out.println("createParallelUsers takes " + (end - start) + "ms");
        return null;
    }

    public List<UserStatus> createStreamUsers(List<User> userList) {
        List<List<User>> mapTrack = new ArrayList<>();
        List<UserStatus> userStatusList = new ArrayList<>();
        for (int i = 0; i < userList.size(); i += commitSize) {
            mapTrack.add(userList.subList(i, Math.min(i + commitSize, userList.size())));
        }
        for (List<User> dividedUsersList : mapTrack) {
            int count = 1;
            System.out.println("createStreamUsers:" + count + ": " + dividedUsersList.size());
            count++;
            long start = System.currentTimeMillis();
            // start of function
            userStatusList.addAll(dividedUsersList.parallelStream()
                    .map(user -> {
                        UserStatus userStatus = new UserStatus();
                        userStatus.setUserEntity(userDtoToEntity(user));
                        userStatus.setIsSuccessful(false);
                        return userStatus;
                    })
                    .map(userStatus -> {
                        try {
                            userRepository.save(userStatus.getUserEntity());
                            userStatus.setIsSuccessful(true);
                        } catch (Exception e) {
                            userStatus.setIsSuccessful(false);
                        }
                        return userStatus;
                    })
                    .collect(Collectors.toList()));
            userRepository.flush();
            long end = System.currentTimeMillis();
            System.out.println("createStreamUsers takes " + (end - start) + "ms");
        }
        // end of function
        return userStatusList;
    }

    public List<UserStatus> processParallel(List<List<User>> listOfUserList) {
        List<CompletableFuture<List<UserStatus>>> collect = listOfUserList.stream().map(this::asyncCall).collect(Collectors.toList());
        List<CompletableFuture<SendResult<String, Object>>> collectPartitioner = listOfUserList.stream().map(this::callKafka).collect(Collectors.toList());
        collectPartitioner.forEach(future -> future.whenComplete((result, ex) -> {
            if(ex != null) {
                System.out.println("failed");
            } else {
                System.out.println("success");
            }
        }));
        List<UserStatus> userStatusList = new ArrayList<>();
        collect.forEach(val -> {
            try {
                userStatusList.addAll(val.get());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        return userStatusList;
    }

    private CompletableFuture<List<UserStatus>> asyncCall(List<User> userList) {
        int count = 1;
        System.out.println("asyncCall:" + count++ + ": " + userList.size());
        count++;
        return CompletableFuture.supplyAsync(() -> {
            return createStreamUsers(userList);
        });
    }*/

}
