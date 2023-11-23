from torch_geometric.datasets import Amazon
import time
import torch
from torch_geometric.nn import GCNConv
import torch.nn.functional as F
import torch.nn as nn
import torch_geometric.transforms as T

start_time = time.time()
#device = "cuda"
device = "cpu"
dataset = Amazon(root='./', name='computers')
graph = dataset[0]
split = T.RandomNodeSplit(num_val=0.1, num_test=0.2)
graph = split(graph)
#graph.to(device)


class GCN(torch.nn.Module):
    def __init__(self):
        super().__init__()
        self.conv1 = GCNConv(dataset.num_node_features, 16)
        self.conv2 = GCNConv(16, dataset.num_classes)

    def forward(self, data):
        x, edge_index = data.x, data.edge_index

        x = self.conv1(x, edge_index)
        x = F.relu(x)
        output = self.conv2(x, edge_index)
        return output
    

# GCN 모델을 학습하는 함수
# 입력으로는 모델, 그래프 데이터, 옵티마이저, 손실함수, 에폭 횟수가 주어짐 
def train_node_classifier(model, graph, optimizer, criterion, n_epochs=200):
    # 에폭 횟수만큼 학습 반복
    for epoch in range(1, n_epochs + 1):
        model.train() # 모델을 학습 상태로 전환
        optimizer.zero_grad() # 그래디언트 초기화
        out = model(graph) # out :예측값
        loss = criterion(out[graph.train_mask], graph.y[graph.train_mask]) # loss 계산
        loss.backward() # Backpropagation 수행
        optimizer.step() # 파라미터 업데이트

        pred = out.argmax(dim=1) #out에서 가장 높은 값을 가지는 인덱스를 예측값으로 사용
        acc = eval_node_classifier(model, graph, graph.val_mask) # 모델 성능 검증

        if epoch % 10 == 0:
            print(f'Epoch: {epoch:03d}, Train Loss: {loss:.3f}, Val Acc: {acc:.3f}')

    return model


# 노드 분류 모델의 성능을 평가하는 함수
# 입력으로는 모델, 그래프, 마스크가 주어짐
def eval_node_classifier(model, graph, mask):

    model.eval() # 모델을 평가 모드로 전환
    # 모델의 출력 계산 -> argmax함수를 사용하여 출력 텐서에서 각 노드의 예측 클래스를 결정
    pred = model(graph).argmax(dim=1)
    # 예측된 클래스와 그래프의 실제 클래스를 비교하여 정확하게 분류된 노드의 수 계산

    correct = (pred[mask] == graph.y[mask]).sum()
    # 정확도 계산
    acc = int(correct) / int(mask.sum())

    return acc
    
gcn = GCN().to(device)
optimizer_gcn = torch.optim.Adam(gcn.parameters(), lr=0.01, weight_decay=5e-4)
criterion = nn.CrossEntropyLoss()
gcn = train_node_classifier(gcn, graph, optimizer_gcn, criterion)

test_acc = eval_node_classifier(gcn, graph, graph.test_mask)


print(f'Test Acc: {test_acc:.3f}')
end_time = time.time()
print("총 소요 시간: %.3f초" %(end_time - start_time))
