import tensorflow as tf

tf.random.set_seed(777)

x_data = [[0 ,0],
         [0, 1],
         [1, 0],
         [1, 1]]
y_data = [[0],
         [1],
         [1],
         [0]]
dataset = tf.data.Dataset.from_tensor_slices((x_data, y_data)).batch(len(x_data))

def preprocess_data(features, labels):
    features = tf.cast(features, tf.float32)
    labels = tf.cast(labels, tf.float32)
    return features, labels
# Layer1의 첫 번째 Unit
W1 = tf.Variable(tf.random.normal([2, 1], 0, 1, tf.float32), name='weight1')
b1 = tf.Variable(tf.random.normal([1], 0, 1, tf.float32), name='bias1')
# Layer1의 두 번째 Unit
W2 = tf.Variable(tf.random.normal([2, 1], 0, 1, tf.float32), name='weight2')
b2 = tf.Variable(tf.random.normal([1], 0, 1, tf.float32), name='bias2')
# Layer2의 Unit
W3 = tf.Variable(tf.random.normal([2, 1], 0, 1, tf.float32), name='weight3')
b3 = tf.Variable(tf.random.normal([1], 0, 1, tf.float32), name='bias3')

# Layer를 연결한 hypothesis
def neural_net(features):
    layer1 = tf.sigmoid(tf.matmul(features, W1) + b1)
    layer2 = tf.sigmoid(tf.matmul(features, W2) + b2)
    layer3 = tf.concat([layer1, layer2], -1)
    layer3 = tf.reshape(layer3, shape = [-1, 2])
    hypothesis = tf.sigmoid(tf.matmul(layer3, W3) + b3)
    return hypothesis

# Logistic Cost Function
def loss_fn(hypothesis, labels):
    # -y*log(H(X))-(1-y)*log(1-H(X))
    cost = -tf.reduce_mean(labels * tf.math.log(hypothesis) + (1 - labels) * tf.math.log(1 - hypothesis))
    return cost

# Gradient Descent
def grad(features, labels):
    with tf.GradientTape() as tape:
        loss_value = loss_fn(neural_net(features), labels)
    return tape.gradient(loss_value, [W1, W2, W3, b1, b2, b3])

# Accuracy 측정
def accuracy_fn(hypothesis, labels):
    predicted = tf.cast(hypothesis > 0.5, dtype=tf.float32)
    accuracy = tf.reduce_mean(tf.cast(tf.equal(predicted, labels), dtype=tf.float32))
    return accuracy

optimizer = tf.keras.optimizers.Adam(learning_rate=0.01)
EPOCHS = 3000

for step in range(EPOCHS):
    for features, labels in dataset:
        features, labels = preprocess_data(features, labels)
        grads = grad(features, labels)
        optimizer.apply_gradients(grads_and_vars=zip(grads, [W1, W2, W3, b1, b2, b3]))
        if step % 500 == 0:
            print("Iter: {}, Loss: {:.4f}".format(step, loss_fn(neural_net(features), labels)))
x_data, y_data = preprocess_data(x_data, y_data)
test_acc = accuracy_fn(neural_net(x_data), y_data)
print("Test Accuracy : {:.4f}".format(test_acc))