package databuilder.data.db_managers.consumer;

import com.flipkart.databuilderframework.model.DataDelta;
import databuilder.data.models.ConsumerDetailsData;
import databuilder.data.models.requests.CreateConsumerRequest;
import databuilder.data.persistence.StoredConsumerDetails;
import io.dropwizard.sharding.dao.RelationalDao;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class DBConsumerManager implements ConsumerManager {

    private final RelationalDao<StoredConsumerDetails> consumerDetailsRelationalDao;

    @Inject
    public DBConsumerManager(RelationalDao<StoredConsumerDetails> consumerDetailsRelationalDao) {
        this.consumerDetailsRelationalDao = consumerDetailsRelationalDao;
    }

    @Override
    public <T> Optional<T> createConsumer(CreateConsumerRequest createConsumerRequest, Function<ConsumerDetailsData, T> handler) {

        try{
            StoredConsumerDetails storedConsumerDetails = StoredConsumerDetails.builder()
                    .consumerName(createConsumerRequest.getConsumerName())
                    .mobileNumber(createConsumerRequest.getMobileNumber())
                    .build();
            Optional<ConsumerDetailsData> result = consumerDetailsRelationalDao.save(createConsumerRequest.getMobileNumber(), storedConsumerDetails)
                    .map(this :: toWired);

            if(result.isPresent())
                new DataDelta(result.get());

            return result.map(handler);
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getConsumer(String mobileNumber, Function<ConsumerDetailsData, T> handler) {
        try{
            return consumerDetailsRelationalDao.select(
                    mobileNumber,
                    DetachedCriteria.forClass(StoredConsumerDetails.class)
                        .add(Restrictions.eq("mobileNumber", mobileNumber)),
                    0,
                    1)
                    .stream()
                    .findFirst()
                    .map(this :: toWired)
                    .map(handler);


        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public <T> List<T> getConsumers(Function<ConsumerDetailsData, T> handler) {
        return consumerDetailsRelationalDao.scatterGather(
                DetachedCriteria.forClass(StoredConsumerDetails.class))
                .stream()
                .map(this :: toWired)
                .map(handler)
                .collect(Collectors.toList()
        );
    }

    public ConsumerDetailsData toWired(StoredConsumerDetails storedConsumerDetails){
        return ConsumerDetailsData.builder()
                .consumerName(storedConsumerDetails.getConsumerName())
                .mobileNumber(storedConsumerDetails.getMobileNumber())
                .build();
    }
}
